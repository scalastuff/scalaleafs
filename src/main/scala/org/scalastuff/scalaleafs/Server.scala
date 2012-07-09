package org.scalastuff.scalaleafs

import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import java.util.LinkedList
import scala.collection.mutable
import java.util.concurrent.atomic.AtomicLong
import scala.xml.NodeSeq
import java.net.URI
case class AjaxCallback(request : InitialRequest, f : Map[String, Seq[String]] => Unit)

object ConfigVar {
  
  // Type def to make sure each individual tuple has matching types for var and value.
  type Assignment[A] = Tuple2[ConfigVar[A], A]

  // Implicitly convert a configVar value to its value. 
  implicit def toValue[A](configVar : ConfigVar[A]) = R.configuration(configVar) 
}

abstract class ConfigVar[A](val defaultValue : A) 

class Configuration (assignments : ConfigVar.Assignment[_]*) {
  
  private val values : Map[ConfigVar[_], Any] = assignments.toMap
  
  // Get the configuration value for given configVar. 
  def apply[A](configVar : ConfigVar[A]) = 
    values.get(configVar).map(_.asInstanceOf[A]).getOrElse(configVar.defaultValue)
}


object DebugMode extends ConfigVar[Boolean](false)

object AjaxCallbackPath extends ConfigVar[String]("leafs/ajaxCallback")
object AjaxFormPostPath extends ConfigVar[String]("leafs/ajaxFormPost")
object ResourcePath extends ConfigVar[String]("leafs/")

class Server(val resourceRoot : Package, val configuration : Configuration = new Configuration) {

  val ajaxCallbackPath = Url.parsePath(configuration(AjaxCallbackPath))
  val ajaxFormPostPath = Url.parsePath(configuration(AjaxFormPostPath))
  val resourcePath = Url.parsePath(configuration(ResourcePath))
}

class ExpiredException(message : String) extends Exception(message)
class InvalidUrlException(url : Url) extends Exception("Invalid url: " + url)

/**
 * Contains session data.
 */
class Session(val server : Server, val configuration : Configuration) {

  private[scalaleafs] val ajaxCallbacks = new ConcurrentHashMap[String, AjaxCallback]()
 
  private[scalaleafs] val callbackIDGenerator = new AtomicLong {
    def generate : String = "cb" + getAndIncrement()
  }
  
  val debugMode = configuration(DebugMode) || System.getProperty("leafsDebugMode") != null
  
  def handleAjaxCallback(callbackId : String, parameters : Map[String, Seq[String]]) : String = {
    mkPostRequestJsString(processAjaxCallback(callbackId, parameters).toSeq)
  }
  
  def processAjaxCallback(callbackId : String, parameters : Map[String, Seq[String]]) : JsCmd = {
    ajaxCallbacks.get(callbackId) match {
      case null => 
        throw new ExpiredException("Callback expired: " + callbackId)
      case ajaxCallback => 
        try {
          val request = new Request(ajaxCallback.request)
          R.set(request)
          ajaxCallback.request.synchronized {
            ajaxCallback.f(parameters)
          }
          request.eagerPostRequestJs & request.postRequestJs 
        } finally {
          R.set(null)
        }
    }
  } 

  def handleAjaxFormPost(parameters : Map[String, Seq[String]]) : String = {
    
    // Separate normal fields from actions.
    val (fields, actions) = parameters.toSeq.partition(_._1 != "action")
        
    // Call callbacks for fields and actions, in order.
    val jsCmds : Seq[JsCmd] =
      // Fields go first that have a single, nameless parameter value.
      fields.map {
        case (callbackId, values) =>
          processAjaxCallback(callbackId, Map("" -> values))
      } ++
      // Actions are executed next, they have no parameters.
      actions.map {
        case (_, Seq(callbackId)) =>
          processAjaxCallback(callbackId, Map.empty)
        case (_, Nil) =>
          Noop
      }

    // Make a result string.
    mkPostRequestJsString(jsCmds)
  } 

  def handleResource(resource : String) : Option[(Array[Byte], ResourceType)] = Resources.resourceContent(resource)
  
  def handleRequest(url : Url, f : () => Unit) {
    try {
      val request = new InitialRequest(this, configuration, url)
      val transientRequest = new Request(request)
      R.set(transientRequest)
      request.synchronized {
        f()
        request._headContributions = transientRequest._headContributions
      }
    } finally {
      R.set(null)
    }
  }
  
  
  def mkPostRequestJsString(jsCmds : Seq[JsCmd]) = 
    jsCmds match {
      case Seq() => ""
      case cmds => cmds.map { cmd => 
        val logCmd = if (debugMode) "console.log(\"Callback result: " + cmd.toString.replace("\"", "'") + "\");\n" else ""
        logCmd + " try { " + cmd + "} catch (e) { console.log(e); };\n"
      }.mkString
    }
}

/**
 * An initial request is created for each http request but shared for each subsequent callback.
 */
class InitialRequest(val session : Session, val configuration : Configuration, private[scalaleafs] var _url : Url) {
  private[scalaleafs] var _headContributions : mutable.Map[String, HeadContribution] = null
  private[scalaleafs] val urlManager = new UrlManager
  
  lazy val resourceBaseUrl = Url(_url.context, session.server.resourcePath, Map.empty)
}

/**
 * A request is created for each http request, including both the initial page request and the subsequent callback calls.
 */
class Request(val initialRequest : InitialRequest) {
  var eagerPostRequestJs : JsCmd = Noop
  var postRequestJs : JsCmd = Noop
  private[scalaleafs] var _headContributions : mutable.Map[String, HeadContribution] = null

  def configuration = initialRequest.configuration
  def session = initialRequest.session
  def server = initialRequest.session.server
  def resourceBaseUrl = initialRequest.resourceBaseUrl
  def debugMode = session.debugMode
  
  /**
   * The current request url.
   */
  def url = initialRequest._url
  
  /**
   * Changes the browser url without a page refresh.
   */
  def changeUrl(uri : String) : Unit = changeUrl(initialRequest._url.resolve(uri))

  /**
   * Changes the browser url without a page refresh.
   */
  def changeUrl(url : Url) : Unit = {
    if (initialRequest._url != url) {
      initialRequest._url = url
      addPostRequestJs(initialRequest.urlManager.handleUrl(url))
    }
  }
  private[scalaleafs] def popUrl(uri : String) = {
    val url = initialRequest._url.resolve(uri)
    if (initialRequest._url != url) {
      initialRequest._url = url
      initialRequest.urlManager.handleUrl(url)
    }
  }

  def headContributions = 
    if (_headContributions == null) Seq.empty
    else _headContributions.values
    
  def addHeadContribution(contribution : HeadContribution) {
    if (_headContributions == null) {
      _headContributions = mutable.Map[String, HeadContribution]()
    }
    
    _headContributions.put(contribution.key, contribution) match {
      case Some(_) =>
      case None => contribution.dependsOn.foreach(dep => addHeadContribution(dep))
    }
  }
  
  def addPostRequestJs(jsCmd : JsCmd) {
    if (jsCmd != Noop) {
      postRequestJs &= jsCmd
    }
  }

  def addEagerPostRequestJs(jsCmd : JsCmd) {
    if (jsCmd != Noop) {
      eagerPostRequestJs &= jsCmd
    }
  }
  
  def callbackId(f : Map[String, Seq[String]] => Unit) : String = {
      val uid = session.callbackIDGenerator.generate
      initialRequest.session.ajaxCallbacks.put(uid, AjaxCallback(initialRequest, f))
      addHeadContribution(JQuery)
      addHeadContribution(LeafsJavaScriptResource)
      uid
  }
  
  def callback(f : Map[String, Seq[String]] => Unit, parameters : (String, JsExp)*) : JsCmd = {
      if (parameters.isEmpty)
        JsCmd("leafs.callback('" + callbackId(f) + "');")
      else 
        JsCmd("leafs.callback('" + callbackId(f) + "?" + parameters.map(x => x._1.toString + "=' + " + x._2.toString).mkString(" + '&") + ");")
  }
}

object R extends ThreadLocal[Request] {
  implicit def toRequest(r : ThreadLocal[Request]) : Request = r.get
  
  /**
   * Throws an exception if thread local hasn't been set.
   */
  override def get = super.get match {
    case null => throw new Exception("No request context")
    case transientRequest => transientRequest
  }
}

