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

import scala.xml.Elem
import scala.xml.NodeSeq
import implicits._
import scala.concurrent.Future

object Html extends Html

trait Html {
 
  def exec(jsCmd : JSCmd) = new RenderNode with NoChildRenderNode {
    def render(context : Context, xml : NodeSeq) = {
      context.addPostRequestJs(jsCmd)
      xml
    } 
  }
  
  def contrib(contrib : HeadContribution*) = new RenderNode with NoChildRenderNode {
    def render(context : Context, xml : NodeSeq) = {
      contrib.foreach(context.addHeadContribution(_))
      xml
    } 
  }
  
  def onclick(f : => JSCmd) : ElemModifier = 
    Xml.setAttr("onclick", { context => 
      context.callback(context => _ => context.addPostRequestJs(f)) & JsReturnFalse
      })
      
  def onchange(f : String => JSCmd) : ElemModifier = 
    Xml.setAttr("onchange", { context => 
      context.callback(JSExp("this.value"))(context => s => context.addPostRequestJs(f(s))) & JsReturnFalse
    })
  
  def select[A](values : Seq[A])(f : A => JSCmd) = {
    Xml.setAttr("onchange", _.callback(JSExp("this.selectedIndex"))(context => s => context.addPostRequestJs(f(values(Integer.parseInt(s)))))) & 
    Xml.setContent(content_ => values.map(v => <option>{v}</option>))
  }
  
  def linkHref = ElemModifier { 
    (context, elem) =>
      XmlHelpers.attr(elem, "href") match {
        case "" => elem
        case href =>
          XmlHelpers.setAttr(XmlHelpers.setAttr(elem, "href", "/" + context.url.resolve(href)), "onclick", (context.callback(context => _ => context.url = href) & JsReturnFalse).toString)
      }          
  }
}