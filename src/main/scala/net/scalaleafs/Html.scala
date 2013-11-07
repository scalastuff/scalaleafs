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
 
  def onclick(f : => JSCmd) : ElemModifier = {
    Xml.setAttr("onclick", { context => 
      context.callback(context => _ => context.addPostRequestJs(f)) & JsReturnFalse
      })
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