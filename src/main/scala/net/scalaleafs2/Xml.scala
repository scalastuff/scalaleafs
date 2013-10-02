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

import scala.xml.NodeSeq
import scala.xml.Elem

object Xml extends Xml

trait Xml {
  
  def mkElem(cssConstructor : CssConstructor) = new ElemRenderNode with RenderLeaf {
    def render(context : Context, xml : NodeSeq) = 
      cssConstructor(xml)
  }
  
  def mkElem(cssConstructor : CssConstructor, condition : => Boolean) = new RenderLeaf {
    def render(context : Context, xml : NodeSeq) = 
      if (condition) cssConstructor(xml)
      else xml
  }

  def mkElem(cssConstructor : CssConstructor, modifier : ElemModifier) = new ElemRenderNode {
    def render(context : Context, xml : NodeSeq) = 
      modifier.render(context, cssConstructor(xml))
      
    def children = Seq(modifier)
  }

  def mkElem(cssConstructor : CssConstructor, modifier : ElemModifier, condition : => Boolean) = new RenderNode {
    def render(context : Context, xml : NodeSeq) = 
      if (condition) modifier.render(context, cssConstructor(xml))
      else xml
      
    def children = Seq(modifier)
  }
  
  /**
   * Transformation that replaces the root element of some input xml with a new Element.
   * The element is created using a css-like syntax (e.g. "div.wrapper input[name='desc']").
   * @see CssConstructor
   */
  def replaceElem(cssConstructor : CssConstructor) : ElemModifier =  
    new ElemModifier((context, elem) => cssConstructor(elem.child), true) 
  
  def replaceElem(cssConstructor : CssConstructor, condition : => Boolean) : ElemModifier = 
    new ElemModifier((context, elem) => cssConstructor(elem.child), condition) 
  
  def replaceElem(cssConstructor : CssConstructor, modifier : ElemModifier) : ElemRenderNode = 
    new ElemModifier((context, elem) => cssConstructor(elem.child), true) & modifier 
  
  def replaceElem(cssConstructor : CssConstructor, modifier : ElemModifier, condition : => Boolean) = new ExpectElemRenderNode with ElemRenderNode {
    def render(context : Context, elem : Elem) : Elem = {
      if (condition) modifier.render(context, cssConstructor(elem.child))
      else elem
    }
    def children = Seq(modifier)
  } 
     
  
  /**
   * Transformation that removes the root element of some input xml.
   */
  def children(condition : => Boolean = true) : ElemRenderNode = new ElemRenderNode {
    def apply(context : Context, elem : Elem) : NodeSeq = 
      if (condition) elem.child
      else elem
  }
  
  /**
   * Transformation that transforms child nodes of some input element.
   */
  def setContent(content : NodeSeq => NodeSeq, condition : => Boolean = true) = 
    new ElemModifier((context, elem) => XmlHelpers.setContent(elem, content(elem.child)), condition) 
  
  /**
   * Transformation that sets an attribute of some input element.
   */
  def setAttr(attr : String, value : String) : ElemModifier = 
    new ElemModifier((context, elem) => XmlHelpers.setAttr(elem, attr, value), true) 
  
  def setAttr(attr : String, value : String, condition : => Boolean) : ElemModifier = 
    new ElemModifier((context, elem) => XmlHelpers.setAttr(elem, attr, value), condition) 
  
  def setAttr(attr : String, value : JSCmd) : ElemModifier = 
    new ElemModifier((context, elem) => XmlHelpers.setAttr(elem, attr, value.toString), true) 
  
  /**
   * Transformation that replaces child nodes of some input element with a text node.
   */
  def setText(value : String, condition : => Boolean = true) = 
    new ElemModifier((context, elem) => XmlHelpers.setText(elem, value), condition) 
  
  /**
   * Transformation that removes an attribute from some input element.
   */
  def removeAttr(attr : String, value : String, condition : => Boolean = true) = 
    new ElemModifier((context, elem) => XmlHelpers.removeAttr(elem, attr), condition) 
  
  /**
   * Transformation that adds a value to an attribute of some input element.
   */
  def addAttrValue(attr : String, value : String, condition : => Boolean = true) = 
    new ElemModifier((context, elem) => XmlHelpers.addAttrValue(elem, attr, value), condition) 
  
  /**
   * Transformation that removes a value from an attribute of some input element.
   */
  def removeAttrValue(attr : String, value : String, condition : => Boolean = true) = 
    new ElemModifier((context, elem) => XmlHelpers.removeAttrValue(elem, attr, value), condition) 
  
  /**
   * Transformation that adds a value to an attribute of some input element.
   */
  def setId(id : String, condition : => Boolean = true) = 
    new ElemModifier((context, elem) => XmlHelpers.setId(elem, id), condition) 
  
  /**
   * Transformation that sets the class attribute of some input element.
   */
  def setClass(className : String, condition : => Boolean = true) = 
    new ElemModifier((context, elem) => XmlHelpers.setClass(elem, className), condition) 
  
  /**
   * Transformation that adds a class to some input element.
   */
  def addClass(className : String, condition : => Boolean = true) = 
    new ElemModifier((context, elem) => XmlHelpers.addClass(elem, className), condition) 
  
  /**
   * Transformation that removes a class from some input element.
   */
  def removeClass(className : String, condition : => Boolean = true) = 
    new ElemModifier((context, elem) => XmlHelpers.removeClass(elem, className), condition) 
}