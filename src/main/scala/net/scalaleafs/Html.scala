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
}