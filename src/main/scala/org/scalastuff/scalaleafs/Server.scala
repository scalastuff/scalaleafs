package org.scalastuff.scalaleafs

import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import java.util.LinkedList
import scala.collection.mutable
import java.util.concurrent.atomic.AtomicLong
import scala.xml.NodeSeq
import implicits._
import java.net.URI
case class AjaxCallback(request : Request, f : Map[String, List[String]] => Unit)


object Server {
  val ajaxCallbackPath = "ajaxCallback"
  val resourcePath = "resource"
  var debugMode = true
}

class Server(val configuration : Configuration = Configuration()) {
  def callbackUrl(uid : String) = configuration.contextPath.mkString("/") + "/" + Server.ajaxCallbackPath + "/" + uid
  def resourceUrl(name : String) = configuration.contextPath.mkString("/") + "/" + Server.resourcePath + "/" + name
  println("Starting leafs")
}

class Session(val server : Server, val configuration : Configuration) {

  private[scalaleafs] val ajaxCallbacks = new ConcurrentHashMap[String, AjaxCallback]()
 
  private[scalaleafs] val callbackIDGenerator = new AtomicLong {
    def generate : String = "cb" + getAndIncrement()
  }
  
  def handleAjaxCallback(callbackId : String, parameters : Map[String, List[String]]) : String = {
    ajaxCallbacks.get(callbackId) match {
      case null => 
        println(ajaxCallbacks.keySet)
        throw new Exception("Expired: " + callbackId)
      case ajaxCallback => {
        try {
          val request = new TransientRequest(ajaxCallback.request)
          R.set(request)
          ajaxCallback.request.synchronized {
            ajaxCallback.f(parameters)
          }
          if (!configuration.debugMode) request.postRequestJs.toString        
          else request.postRequestJs.toSeq.map(cmd => "console.log(\"Callback result: " + cmd.toString.replace("\"", "'") + "\"); try { " + cmd + "} catch (e) { console.log(e); };").mkString
        } finally {
          R.set(null)
        }
      }
    }
  } 

  def handleResource(resource : String) = Resources.resourceContent(resource)

  def handleRequest(url : Url, f : () => Unit)  {
    try {
      val request = new Request(this, configuration, url)
      val transientRequest = new TransientRequest(request)
      R.set(transientRequest)
      request.synchronized {
        f()
        request._headContributions = transientRequest._headContributions
      }
    } finally {
      R.set(null)
    }
  }
}

class Request(val session : Session, val configuration : Configuration, private[scalaleafs] var _url : Url) extends UrlManager {
  private[scalaleafs] var _headContributions : mutable.Map[String, HeadContribution] = null
  
  val setUrlCallbackId = session.callbackIDGenerator.generate 
}

class TransientRequest(val request : Request) {
  var postRequestJs : JsCmd = Noop
  private[scalaleafs] var _headContributions : mutable.Map[String, HeadContribution] = null

  def configuration = request.configuration
  def session = request.session

  def url = request._url
  def url_=(uri : String) : Unit = url_=(request._url.resolve(uri))
  def url_=(uri : URI) : Unit = url_=(request._url.resolve(uri))
  def url_=(url : Url) : Unit = {
    if (request._url != url) {
      request._url = url
      addPostRequestJs(request.handleUrl(url))
    }
  }
  private[scalaleafs] def popUrl(uri : String) = {
    val url = request._url.resolve(uri)
    if (request._url != url) {
      request._url = url
      request.handleUrl(url)
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
      println("adding js:" + jsCmd)
      postRequestJs &= jsCmd
    }
  }

  def registerAjaxCallback(f : Map[String, List[String]] => Unit) : String = {
      val uid = session.callbackIDGenerator.generate
      println("putting in cache: " + uid);
      request.session.ajaxCallbacks.put(uid, AjaxCallback(request, f))
      addHeadContribution(JQuery)
      addHeadContribution(Callback)
      uid
  }
}

object R extends ThreadLocal[TransientRequest] {
  implicit def toTransientRequest(r : ThreadLocal[TransientRequest]) = r.get
  implicit def toRequest(r : ThreadLocal[TransientRequest]) = r.get.request
  
  /**
   * Need to define here to avoid ambiguous implicit conversion. 
   */
  def configuration = get.configuration

  /**
   * Need to define here to avoid ambiguous implicit conversion. 
   */
  def session = get.session
  
  /**
   * Throws an exception if thread local hasn't been set.
   */
  override def get = super.get match {
    case null => throw new Exception("No request context")
    case transientRequest => transientRequest
  }
}

