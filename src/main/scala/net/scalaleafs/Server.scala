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

import net.scalaleafs.AjaxCallbackPath;
import net.scalaleafs.AjaxFormPostPath;
import net.scalaleafs.ContextPath;
import net.scalaleafs.DebugClassLoaderInstantiator;
import net.scalaleafs.DebugMode;
import net.scalaleafs.HeadContributions;
import net.scalaleafs.ResourceFactory;
import net.scalaleafs.ResourcePath;
import net.scalaleafs.Resources;
import net.scalaleafs.Url;
import grizzled.slf4j.Logging

case class AjaxCallback(request : InitialRequest, f : Map[String, Seq[String]] => Unit)

object DebugMode extends ConfigVar[Boolean](false)

object AjaxCallbackPath extends ConfigVar[String]("leafs/ajaxCallback")
object AjaxFormPostPath extends ConfigVar[String]("leafs/ajaxFormPost")
object ResourcePath extends ConfigVar[String]("leafs/")
object ContextPath extends ConfigVar[List[String]](Nil)

class Server(root : Class[_ <: Template], val configuration : Configuration) extends Logging {

  val contextPath = configuration(ContextPath)
  
  val substitutions = Map[String, String] (
      "CONTEXT_PATH" -> contextPath.mkString("/"),
      "AJAX_CALLBACK_PATH" -> (contextPath :+ configuration(AjaxCallbackPath)).mkString("/"),
      "AJAX_FORMPOST_PATH" -> (contextPath :+ configuration(AjaxFormPostPath)).mkString("/"),
      "RESOURCE_PATH" -> (contextPath :+ configuration(ResourcePath)).mkString("/"))

  val debugMode = configuration(DebugMode) || System.getProperty("leafsDebugMode") != null
  
  val resources = new Resources(configuration(ResourceFactory), substitutions, debugMode)
  
  val ajaxCallbackPath = Url.parsePath(configuration(AjaxCallbackPath))
  val ajaxFormPostPath = Url.parsePath(configuration(AjaxFormPostPath))
  val resourcePath = Url.parsePath(configuration(ResourcePath))

  def handleResource(resource : String) : Option[(Array[Byte], ResourceType)] = 
    resources.resourceContent(resource)   
  

  protected def postProcess(request : Request, xml : NodeSeq) = 
    HeadContributions.render(request, xml)

  private lazy val instantiator = 
    new DebugClassLoaderInstantiator(root.getPackage.getName)
  
  if (debugMode) {
    debug("Enabled dynamic classloading for package " + root.getPackage.getName)
  }
  
  protected def processRoot(request : Request) = {
    val template = 
      if (debugMode) instantiator.instantiate(root)
      else root.newInstance
      val pre = template.render
      postProcess(request, pre)
  }
}

