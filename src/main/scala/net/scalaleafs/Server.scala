/**
 * Copyright (c) 2012 Ruud Diterwich.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package net.scalaleafs

import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import java.util.LinkedList
import scala.collection.mutable
import java.util.concurrent.atomic.AtomicLong
import scala.xml.NodeSeq
import java.net.URI

case class AjaxCallback(request : InitialRequest, f : Map[String, Seq[String]] => Unit)

object DebugMode extends ConfigVar[Boolean](false)

object AjaxCallbackPath extends ConfigVar[String]("leafs/ajaxCallback")
object AjaxFormPostPath extends ConfigVar[String]("leafs/ajaxFormPost")
object ResourcePath extends ConfigVar[String]("leafs/")

class Server(val contextPath : List[String], val configuration : Configuration) {

  val substitutions = Map[String, String] (
      "CONTEXT_PATH" -> contextPath.mkString("/"),
      "AJAX_CALLBACK_PATH" -> contextPath.mkString("", "/", "/" + configuration(AjaxCallbackPath)),
      "AJAX_FORMPOST_PATH" -> contextPath.mkString("", "/", "/" + configuration(AjaxFormPostPath)),
      "RESOURCE_PATH" -> contextPath.mkString("", "/", "/" + configuration(ResourcePath)))

  val debugMode = configuration(DebugMode) || System.getProperty("leafsDebugMode") != null

  val resources = new Resources(configuration(ResourceFactory), substitutions, debugMode)
  
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

  def handleResource(resource : String) : Option[(Array[Byte], ResourceType)] = server.resources.resourceContent(resource)
  
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
        val logCmd = if (server.debugMode) "console.log(\"Callback result: " + cmd.toString.replace("\"", "'") + "\");\n" else ""
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
  def debugMode = session.server.debugMode
  
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

/**
 * Global entry point to ScalaLeafs context. R returns the current request, from which the current InitialRequest,
 * the Session and the Server can be reached.
 */
object R extends ThreadLocal[Request] {
  implicit def toRequest(r : ThreadLocal[Request]) : Request = r.get
  
  /**
   * Overridden to throw an exception if thread local hasn't been set.
   */
  override def get = super.get match {
    case null => throw new Exception("No request context")
    case transientRequest => transientRequest
  }
}

