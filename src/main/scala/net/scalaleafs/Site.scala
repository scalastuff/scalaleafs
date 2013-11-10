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
import grizzled.slf4j.Logging
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

case class AjaxCallback(f : Context => Seq[(String, String)] => Future[Unit])

object DebugMode extends ConfigVal[Boolean](false)

object AjaxCallbackPath extends ConfigVal[String]("leafs/ajaxCallback")
object AjaxFormPostPath extends ConfigVal[String]("leafs/ajaxFormPost")
object ResourcePath extends ConfigVal[String]("leafs/")
object CallbackIDGenerator extends ConfigVal[Function0[String]](() => "cb" + UUID.randomUUID)

class Site(rootTemplateClass : Class[_ <: Template], val contextPath : List[String], _configuration : Configuration)(implicit val executionContext : ExecutionContext) extends Logging {

  implicit def configuration = _configuration
  private[scalaleafs] val windows = TrieMap[String, Window]()

  val debugMode = DebugMode.get || System.getProperty("leafsDebugMode") == "true"

  private val callbackIdGenerator : () => String = CallbackIDGenerator(configuration)
  private val rootTemplateInstantiator = new RootTemplateInstantiator(rootTemplateClass, rootTemplateClass.getPackage.getName, debugMode)
  
  val ajaxCallbackPath = contextPath ++ Url.parsePath(AjaxCallbackPath.get)
  val ajaxFormPostPath = contextPath ++ Url.parsePath(AjaxFormPostPath.get)
  val resourcePath = contextPath ++ Url.parsePath(ResourcePath.get)

  val substitutions = Map[String, String] (
    "CONTEXT_PATH" -> contextPath.mkString("/"),
    "AJAX_CALLBACK_PATH" -> ajaxCallbackPath.mkString("/"),
    "AJAX_FORMPOST_PATH" -> ajaxFormPostPath.mkString("/"),
    "RESOURCE_PATH" -> resourcePath.mkString("/"))

  val resources = new Resources(ResourceFactory.get, substitutions, debugMode)

  def handleRequest[A](url : Url, requestVals : RequestVal.Assignment[_]*): Future[String] = {
    val window = new Window(this, url, rootTemplateInstantiator)
    windows += window.id -> window
    val start = System.currentTimeMillis()
    window.handleRequest(url, requestVals:_*).andThen {
      case _ => debug("request: " + url.path + " (" + (System.currentTimeMillis - start) + " ms)")
    }
  }
  
  def handleAjaxCallback(windowId : String, callbackId : String, parameters : Seq[(String, String)], requestVals : RequestVal.Assignment[_]*) : Future[String] = {
    val start = System.currentTimeMillis()
    windows.get(windowId) match {
      case Some(window) => window.handleAjaxCallback(callbackId, parameters, requestVals:_*).andThen {
        case _ => debug("callback: " + callbackId+  " (" + (System.currentTimeMillis - start) + " ms)")
    }
      case None => 
        debug("Window expired: " + windowId)
        Future.successful(Noop)
    }
  }

  def handleResource(resource : String) : Option[(Array[Byte], ResourceType)] = 
    resources.resourceContent(resource)   
  
  def generateCallbackID : String = 
    callbackIdGenerator()
  
  private[scalaleafs] def mkPostRequestJsString(JSCmds : Seq[JSCmd]) = 
    JSCmds match {
      case Seq() => ""
      case cmds => cmds.map { cmd => 
        val logCmd = if (debugMode) "console.log(\"Callback result command: " + cmd.toString.replace("\"", "'") + "\");\n" else ""
        logCmd + " try { " + cmd + "} catch (e) { console.log(e); };\n"
      }.mkString
    }
}
