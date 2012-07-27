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
  
  def handleRequest(url : Url, f : () => Unit) {
    try {
      val initialRequest = new InitialRequest(this, configuration, url)
      val request = new Request(initialRequest, true)
      R.set(request)
      initialRequest.synchronized {
        f()
        initialRequest._headContributionKeys ++= request._headContributionKeys
      }
    } finally {
      R.set(null)
    }
  }

  def processAjaxCallback(callbackId : String, parameters : Map[String, Seq[String]]) : JSCmd = {
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

  def handleResource(resource : String) : Option[(Array[Byte], ResourceType)] = server.resources.resourceContent(resource)   
  
  def mkPostRequestJsString(JSCmds : Seq[JSCmd]) = 
    JSCmds match {
      case Seq() => ""
      case cmds => cmds.map { cmd => 
        val logCmd = if (server.debugMode) "console.log(\"Callback result: " + cmd.toString.replace("\"", "'") + "\");\n" else ""
        logCmd + " try { " + cmd + "} catch (e) { console.log(e); };\n"
      }.mkString
    }
}

