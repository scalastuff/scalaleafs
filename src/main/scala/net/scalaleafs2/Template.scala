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
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.{XML, NodeSeq, Elem}
import org.xml.sax.SAXParseException
import scala.concurrent.Future

/**
 * A template is an XmlTransformation that reads its input, by default, from a class-path resource, and provides
 * a bind hook to transform this input to some output.
 * Class-path resources are cached (when not in debug mode) in a JVM-global cache.
 */
trait Template extends RenderNode with SingleChildRenderNode with Xml with Html with Binding {
  
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

  /**
   * Override to define the render tree.
   */
  def render : RenderNode
  
  /**
   * Initial input value. 
   * Override this value to prevent reading of input.
   */  
  val input : NodeSeq = NodeSeq.Empty

  /**
   * Reads input of this template.
   * Default implementation reads a template resource from the classpath
   * with the same name as the class of this template.
   */
  def readInput(context : Context) : NodeSeq = 
    Template.template(context, getClass) 
    
  def render(context : Context, xml : NodeSeq) = child.render(context, input(context))
  def renderChanges(context : Context) = child.renderChanges(context)
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
}


