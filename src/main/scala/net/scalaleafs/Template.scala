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

import scala.xml.Elem
import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.XML

import org.xml.sax.SAXParseException

/**
 * A template is an XmlTransformation that reads its input, by default, from a class-path resource, and provides
 * a bind hook to transform this input to some output.
 * Class-path resources are cached (when not in debug mode) in a JVM-global cache.
 */
trait Template extends RenderNode with SingleChildRenderNode with Xml with Html with Binding {
  
  private val READ_INPUT = <h1>Read Input</h1>
  private val READ_JS = "READ_JS"
  
  implicit def context : Context = Context.get
  
  lazy val child : RenderNode = render
  private var _input : NodeSeq = null
  private def input(context : Context) : NodeSeq = {
    if (_input == null) {
      _input = input
      if (_input == NodeSeq.Empty)
        _input = readInput(context)
    }
    _input
  }

  private var _js : Option[HeadContribution] = null
  private def js(context : Context) : Option[HeadContribution] = {
    if (_js == null) {
      _js = Template.js(context, getClass)
    }
    _js
  }
  
  /**
   * Override to define the render tree.
   */
  def render : RenderNode
  
  /**
   * Initial input value. 
   * Override this value to prevent reading of input.
   */  
  protected val input : NodeSeq = NodeSeq.Empty

  /**
   * Reads input of this template.
   * Default implementation reads a template resource from the classpath
   * with the same name as the class of this template.
   */
  protected def readInput(context : Context) : NodeSeq = 
    Template.template(context, getClass) 
    
  def render(context : Context, xml : NodeSeq) = {
    js(context).foreach(context.addHeadContribution(_))
    child.render(context, input(context))
  }
}

object Template {

  val templateCache = new ConcurrentHashMap[Class[_], NodeSeq]
  def template(context : Context, c : Class[_]) : NodeSeq = {
    var xml = templateCache.get(c)
    if (xml == null) {
      val resourceName = c.getName().replace('.', '/') + ".html";
      try {
        val resource = c.getClassLoader.getResource(resourceName)
        if (resource == null) 
          throw new Exception("Template not found on classpath: " + resourceName)
        val is = resource.openStream
        try {
          xml = XML.load(is) match {
            case elem : Elem if elem.label == "dummy" => elem.child
            case xml => xml
          }
        }
        finally {
          is.close
        }
      } catch {
        case t : SAXParseException if t.getLineNumber >= 0 => throw new Exception("Error in template " + resourceName + " (line " + t.getLineNumber + "): " + t.getLocalizedMessage, t)
        case t : Throwable => throw new Exception("Error in template " + resourceName + ": " + t, t)
      }
      if (!context.debugMode) {
        templateCache.put(c, xml)
      }
    }
    xml
  }
  
  val jsCache = new ConcurrentHashMap[Class[_], Option[HeadContribution]]
  def js(context : Context, c : Class[_]) : Option[HeadContribution] = {
    var s = jsCache.get(c)
    if (s == null) {
      s = try {
        val resourceName = c.getSimpleName + ".js"
        Some(new JavaScript(resourceName, context.site.resources.hashedResourcePathFor(c, resourceName)))
      } catch {
        case t : Throwable => 
          None
      }
      if (!context.debugMode) {
        jsCache.put(c, s)
      }
    }
    s
  }}


