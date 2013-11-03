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
package net.scalaleafs2

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

case class AjaxCallback(f : Context => Map[String, Seq[String]] => Future[Unit])

object DebugMode extends ConfigVar[Boolean](false)

object AjaxCallbackPath extends ConfigVar[String]("leafs/ajaxCallback")
object AjaxFormPostPath extends ConfigVar[String]("leafs/ajaxFormPost")
object ResourcePath extends ConfigVar[String]("leafs/")
object CallbackIDGenerator extends ConfigVar[Function0[String]](() => "cb" + UUID.randomUUID)

class Site(rootTemplateClass : Class[_ <: Template], val contextPath : List[String], _configuration : Configuration)(implicit val executionContext : ExecutionContext) extends Logging {

  implicit def configuration = _configuration
  private[scalaleafs2] val windows = TrieMap[String, Window]()

  val debugMode = DebugMode || System.getProperty("leafsDebugMode") != "false"

  private val callbackIdGenerator : () => String = CallbackIDGenerator(configuration)
  private val rootTemplateInstantiator = new RootTemplateInstantiator(rootTemplateClass, rootTemplateClass.getPackage.getName, debugMode)
  
  val ajaxCallbackPath = contextPath ++ Url.parsePath(AjaxCallbackPath)
  val ajaxFormPostPath = contextPath ++ Url.parsePath(AjaxFormPostPath)
  val resourcePath = contextPath ++ Url.parsePath(ResourcePath)

  val substitutions = Map[String, String] (
    "CONTEXT_PATH" -> contextPath.mkString("/"),
    "AJAX_CALLBACK_PATH" -> ajaxCallbackPath.mkString("/"),
    "AJAX_FORMPOST_PATH" -> ajaxFormPostPath.mkString("/"),
    "RESOURCE_PATH" -> resourcePath.mkString("/"))

  val resources = new Resources(ResourceFactory, substitutions, debugMode)

  def handleRequest[A](url : Url): Future[String] = {
    val window = new Window(this, url, rootTemplateInstantiator)
    windows += window.id -> window
    val start = System.currentTimeMillis()
    window.handleRequest(url).andThen {
      case _ => println("request: " + url.path + " (" + (System.currentTimeMillis - start) + " ms)")
    }
  }
  
  def handleAjaxCallback(windowId : String, callbackId : String, parameters : Map[String, Seq[String]]) : Future[String] = {
    val start = System.currentTimeMillis()
    windows.get(windowId) match {
      case Some(window) => window.handleAjaxCallback(callbackId, parameters).andThen {
        case _ => println("callback: " + callbackId+  " (" + (System.currentTimeMillis - start) + " ms)")
    }
      case None => throw new ExpiredException("Window expired: " + windowId)
    }
  }

  def handleResource(resource : String) : Option[(Array[Byte], ResourceType)] = 
    resources.resourceContent(resource)   
  
  def generateCallbackID : String = 
    callbackIdGenerator()
  
//  private lazy val rootTemplateInstantiator = {
//    val packageName = rootTemplateClass.getPackage.getName
//    debug("Enabled dynamic classloading for package " + packageName)
//    new DebugClassLoaderInstantiator(packageName)
//  }
//  
//  private[scalaleafs2] def rootTemplate = { window : Window =>
//    var instance : Template = null
//    () => {
//      if (debugMode && (instance == null || rootTemplateInstantiator.isOutdated))
//        instance = rootTemplateInstantiator.instantiate(rootTemplateClass)
//      else if (instance == null) 
//        instance = rootTemplateClass.newInstance
//      instance
//    }
//  }
  
  private[scalaleafs2] def mkPostRequestJsString(JSCmds : Seq[JSCmd]) = 
    JSCmds match {
      case Seq() => ""
      case cmds => cmds.map { cmd => 
        val logCmd = if (debugMode) "console.log(\"Callback result command: " + cmd.toString.replace("\"", "'") + "\");\n" else ""
        logCmd + " try { " + cmd + "} catch (e) { console.log(e); };\n"
      }.mkString
    }
}

