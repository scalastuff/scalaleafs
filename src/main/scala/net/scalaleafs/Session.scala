package net.scalaleafs

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import net.scalaleafs.ExpiredException;
import net.scalaleafs.InitialRequest;
import net.scalaleafs.Noop;
import net.scalaleafs.R;
import net.scalaleafs.Request;

/**
 * Contains session data.
 */
class Session(val server : Server, val configuration : Configuration) {

  private[scalaleafs] val ajaxCallbacks = new ConcurrentHashMap[String, AjaxCallback]()
 
  private[scalaleafs] val callbackIDGenerator = new Object {
    def generate : String = "cb" + UUID.randomUUID
  }
  
  def handleAjaxCallback(callbackId : String, parameters : Map[String, Seq[String]]) : String = {
    mkPostRequestJsString(processAjaxCallback(callbackId, parameters).toSeq)
  }
  
  def handleRequest[A](url : Url)(f : Request => A) : A = {
    try {
      val initialRequest = new InitialRequest(this, configuration, url)
      val request = new Request(initialRequest, true)
      R.set(request)
      initialRequest.synchronized {
        val result = f(request)
        initialRequest._headContributionKeys ++= request._headContributionKeys
        result
      }
    } finally {
      R.set(null)
    }
  }

  private def processAjaxCallback(callbackId : String, parameters : Map[String, Seq[String]]) : JSCmd = {
    ajaxCallbacks.get(callbackId) match {
      case null => 
        throw new ExpiredException("Callback expired: " + callbackId)
      case ajaxCallback => 
        try {
          val initialRequest = ajaxCallback.request
          val request = new Request(initialRequest, false)
          R.set(request)
          ajaxCallback.request.synchronized {
            // Call the callback.
            ajaxCallback.f(parameters)
            // Render additional head contributions.
            request._headContributions.foreach(c => request.addEagerPostRequestJs(c.renderAdditional(request)))
            // Store all current head contributions in the initial request. 
            initialRequest._headContributionKeys ++= request._headContributionKeys
          }
          // Process eager JS first, normal JS later
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
    val JSCmds : Seq[JSCmd] =
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
    mkPostRequestJsString(JSCmds)
  } 

  def mkPostRequestJsString(JSCmds : Seq[JSCmd]) = 
    JSCmds match {
      case Seq() => ""
      case cmds => cmds.map { cmd => 
        val logCmd = if (server.debugMode) "console.log(\"Callback result command: " + cmd.toString.replace("\"", "'") + "\");\n" else ""
        logCmd + " try { " + cmd + "} catch (e) { console.log(e); };\n"
      }.mkString
    }
}

