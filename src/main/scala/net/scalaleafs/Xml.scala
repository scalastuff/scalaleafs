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

import scala.xml.NodeSeq
import scala.xml.Elem
import scala.xml.Text
import scala.concurrent.Future

object Xml extends Xml

trait Xml {
  
  def mkElem(cssConstructor : CssConstructor) = new RenderNode with NoChildRenderNode {
    def render(context : Context, xml : NodeSeq) = 
      cssConstructor(xml)
  }
  
  def mkElem(cssConstructor : CssConstructor, condition : => Boolean) = new RenderNode with NoChildRenderNode {
    def render(context : Context, xml : NodeSeq) = 
      if (condition) cssConstructor(xml)
      else xml
  }

  def mkElem(cssConstructor : CssConstructor, modifier : ElemModifier) = new RenderNode with SingleChildRenderNode {
    def child = modifier
    def render(context : Context, xml : NodeSeq) = 
      modifier.render(context, cssConstructor(xml))
  }

  def mkElem(cssConstructor : CssConstructor, modifier : ElemModifier, condition : => Boolean) = new RenderNode with SingleChildRenderNode {
    def child = modifier
    def render(context : Context, xml : NodeSeq) = 
      if (condition) modifier.render(context, cssConstructor(xml))
      else xml
  }

  /**
   * Transformation that replaces the input xml with some static text.
   */
  def replaceWithString(text : => String) : RenderNode = 
    replaceWith(Text(text))

  /**
   * Transformation that replaces the input xml with some static xml.
   */
  def replaceWith(xml : => NodeSeq) = new RenderNode with NoChildRenderNode {
    def render(context : Context, ignore : NodeSeq) = xml
  }
  
  /**
   * Transformation that replaces the root element of some input xml with a new Element.
   * The element is created using a css-like syntax (e.g. "div.wrapper input[name='desc']").
   * @see CssConstructor
   */
  def replaceElem(cssConstructor : CssConstructor) : ElemModifier =  
    ElemModifier((_, elem) => cssConstructor(elem.child)) 
  
  def replaceElem(cssConstructor : CssConstructor, modifier : ElemModifier) : ElemModifier = 
    ElemModifier((context, elem) => modifier.render(context, cssConstructor(elem.child)))
  
  /**
   * Transformation that removes the root element of some input xml.
   */
  def children = new ExpectElemRenderNode with NoChildRenderNode {
    def render(context : Context, elem : Elem) : NodeSeq = 
      elem.child
  }
  
  def replaceContent(_child : RenderNode) = new ExpectElemRenderNode with SingleChildRenderNode {
    def child = _child
    def render(context : Context, elem : Elem) : NodeSeq = 
      elem.copy(child = child.render(context, elem.child))
  }
  
  /**
   * Transformation that transforms child nodes of some input element.
   */
  def setContent(content : NodeSeq => NodeSeq) = 
    ElemModifier((context, elem) => XmlHelpers.setContent(elem, content(elem.child))) 
  
  /**
   * Transformation that sets an attribute of some input element.
   */
  def setAttr(attr : String, f : => String) : ElemModifier = 
    ElemModifier((context, elem) => XmlHelpers.setAttr(elem, attr, f))
  
  def setAttr(attr : String, f : Context => String) : ElemModifier = 
    ElemModifier((context, elem) => XmlHelpers.setAttr(elem, attr, f(context)))

  /**
   * Transformation that replaces child nodes of some element with a text node.
   */
  def setText(f : => String) = 
    ElemModifier((context, elem) => XmlHelpers.setText(elem, f)) 
  
  /**
   * Transformation that removes an attribute from some input element.
   */
  def removeAttr(attr : String, value : String) = 
    ElemModifier((context, elem) => XmlHelpers.removeAttr(elem, attr)) 
  
  /**
   * Transformation that adds a value to an attribute of some input element.
   */
  def addAttrValue(attr : String, value : String) = 
    ElemModifier((context, elem) => XmlHelpers.addAttrValue(elem, attr, value)) 
  
  /**
   * Transformation that removes a value from an attribute of some input element.
   */
  def removeAttrValue(attr : String, value : String) = 
    ElemModifier((context, elem) => XmlHelpers.removeAttrValue(elem, attr, value)) 
  
  /**
   * Transformation that adds a value to an attribute of some input element.
   */
  def setId(id : String) = 
    ElemModifier((context, elem) => XmlHelpers.setId(elem, id)) 
  
  /**
   * Transformation that sets the class attribute of some input element.
   */
  def setClass(className : String) = 
    ElemModifier((context, elem) => XmlHelpers.setClass(elem, className)) 
  
  /**
   * Transformation that adds a class to some input element.
   */
  def addClass(className : String) = 
    ElemModifier((context, elem) => XmlHelpers.addClass(elem, className)) 
  
  /**
   * Transformation that removes a class from some input element.
   */
  def removeClass(className : String) = 
    ElemModifier((context, elem) => XmlHelpers.removeClass(elem, className)) 
}