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

import net.scalaleafs.JsReturnFalse;
import net.scalaleafs.R;
import net.scalaleafs.Xml;
import net.scalaleafs.XmlHelpers;
import scala.xml.Elem
import scala.xml.NodeSeq
import implicits._

object Html extends Html

trait Html {
 
  def onclick(f : => JSCmd) : ElemModifier = {
    Xml.setAttr("onclick", R.callback(_ => R.addPostRequestJs(f)) & JsReturnFalse)
  }
  
  def onchange(f : String => JSCmd) : ElemModifier = {
    Xml.setAttr("onchange", R.callback(JSExp("this.value"))(s => f(s)))
  }
  
  def select[A](values : Seq[A])(f : A => JSCmd) = {
    Xml.setAttr("onchange", R.callback(JSExp("this.selectedIndex"))(s => f(values(Integer.parseInt(s))))) & 
    Xml.setContent(content_ => values.map(v => <option>{v}</option>))
  }
  
  def linkup = new ElemTransformation { 
    def apply(elem : Elem) = {
      XmlHelpers.attr(elem, "href") match {
        case "" => elem
        case href =>
          //XmlHelpers.setAttr(XmlHelpers.setAttr(elem, href, "CTX/" + href), "onclick", R.callback(_ => R.changeUrl(href)).toString)
          XmlHelpers.setAttr(elem, href, "CTX/" + href)
      }          
    }
  }
}