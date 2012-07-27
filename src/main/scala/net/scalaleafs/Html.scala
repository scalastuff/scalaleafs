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

object Html {
 
  def onclick(f : => JSCmd) : ElemModifier = {
    Xml.setAttr("onclick", R.callback(_ => R.addPostRequestJs(f)).toString + " return false;")
  }

//  def onchange(f : String => JSCmd) : ElemModifier = {
//    SetAttr("onchange", R.callback(s => R.addPostRequestJs(s)).toString + " return false;")
//  }
}