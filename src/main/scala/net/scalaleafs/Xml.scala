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


/**
 * Transformation that wraps some input xml with a new Element. 
 * The element is created using a css-like syntax (e.g. "div.wrapper input[name='desc']").
 * @see CssConstructor
 */
class MkElem(cssConstructor : String, condition : => Boolean, modifier : ElemModifier) extends XmlTransformation {
  override def apply(xml : NodeSeq) : NodeSeq = {
    if (condition) modifier(CssConstructor(cssConstructor)(xml)) else xml
  }
}

object Xml extends Xml

trait Xml {
  
  def mkElem(cssConstructor : String) : MkElem =  
    new MkElem(cssConstructor, true, Ident) 

  def mkElem(cssConstructor : String, condition : => Boolean) : MkElem = 
    new MkElem(cssConstructor, condition, Ident) 

  def mkElem(cssConstructor : String, modifier : ElemModifier) : MkElem = 
    new MkElem(cssConstructor, true, modifier) 

  def mkElem(cssConstructor : String, condition : => Boolean, modifier : ElemModifier) : MkElem = 
    new MkElem(cssConstructor, condition, modifier) 
  
  /**
   * Transformation that replaces the root element of some input xml with a new Element.
   * The element is created using a css-like syntax (e.g. "div.wrapper input[name='desc']").
   * @see CssConstructor
   */
  def replaceElem(cssConstructor : String) : ElemModifier =  
    new ElemModifier(e => CssConstructor(cssConstructor)(e.child), true) 
  
  def replaceElem(cssConstructor : String, condition : => Boolean) : ElemModifier = 
    new ElemModifier(e => CssConstructor(cssConstructor)(e.child), condition) 
  
  def replaceElem(cssConstructor : String, modifier : ElemModifier) : ElemModifier = 
    new ElemModifier(e => modifier(CssConstructor(cssConstructor)(e.child)), true) 
  
  def replaceElem(cssConstructor : String, condition : => Boolean, modifier : ElemModifier) : ElemModifier = 
    new ElemModifier(e => modifier(CssConstructor(cssConstructor)(e.child)), condition) 
  
  /**
   * Transformation that removes the root element of some input xml.
   */
  def children(condition : => Boolean = true) : ElemTransformation = new ElemTransformation {
    override def apply(elem : Elem) : NodeSeq = 
      if (condition) elem.child
      else elem
  }
  
  /**
   * Transformation that transforms child nodes of some input element.
   */
  def setContent(content : NodeSeq => NodeSeq, condition : => Boolean = true) = 
    new ElemModifier(xml => XmlHelpers.setContent(xml, content(xml.child)), condition) 
  
  /**
   * Transformation that sets an attribute of some input element.
   */
  def setAttr(attr : String, value : String) : ElemModifier = 
    new ElemModifier(XmlHelpers.setAttr(_, attr, value), true) 
  
  def setAttr(attr : String, value : String, condition : => Boolean) : ElemModifier = 
    new ElemModifier(XmlHelpers.setAttr(_, attr, value), condition) 
  
  def setAttr(attr : String, value : JSCmd) : ElemModifier = 
    new ElemModifier(XmlHelpers.setAttr(_, attr, value.toString), true) 
  
  /**
   * Transformation that replaces child nodes of some input element with a text node.
   */
  def setText(value : String, condition : => Boolean = true) = 
    new ElemModifier(XmlHelpers.setText(_, value), condition) 
  
  /**
   * Transformation that removes an attribute from some input element.
   */
  def removeAttr(attr : String, value : String, condition : => Boolean = true) = 
    new ElemModifier(XmlHelpers.removeAttr(_, attr), condition) 
  
  /**
   * Transformation that adds a value to an attribute of some input element.
   */
  def AddAttrValue(attr : String, value : String, condition : => Boolean = true) = 
    new ElemModifier(XmlHelpers.addAttrValue(_, attr, value), condition) 
  
  /**
   * Transformation that removes a value from an attribute of some input element.
   */
  def removeAttrValue(attr : String, value : String, condition : => Boolean = true) = 
    new ElemModifier(XmlHelpers.removeAttrValue(_, attr, value), condition) 
  
  /**
   * Transformation that adds a value to an attribute of some input element.
   */
  def setId(id : String, condition : => Boolean = true) = 
    new ElemModifier(XmlHelpers.setId(_, id), condition) 
  
  /**
   * Transformation that sets the class attribute of some input element.
   */
  def setClass(className : String, condition : => Boolean = true) = 
    new ElemModifier(XmlHelpers.setClass(_, className), condition) 
  
  /**
   * Transformation that adds a class to some input element.
   */
  def addClass(className : String, condition : => Boolean = true) = 
    new ElemModifier(XmlHelpers.addClass(_, className), condition) 
  
  /**
   * Transformation that removes a class from some input element.
   */
  def removeClass(className : String, condition : => Boolean = true) = 
    new ElemModifier(XmlHelpers.removeClass(_, className), condition) 
}