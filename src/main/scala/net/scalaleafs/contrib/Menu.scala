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
package net.scalaleafs.contrib

import scala.xml.NodeSeq
import net.scalaleafs._

case class MenuItem(path : List[String], text : String, content : UrlTrail => XmlTransformation) {
  val itemPathString = path.mkString("/")
} 

case class Menu(items : MenuItem*) {
  
  // Longest paths first.
  private val sortedItems = items.sortWith(_.path.size > _.path.size)
  
  def render(trail : Var[UrlTrail], contentSelector : String, menuItemSelector : String, selectedClass : String = "selected") = { 
    val selectedItem = trail map { trail =>
      (trail, sortedItems.find(item => trail.remainder.startsWith(item.path)))
    }
    contentSelector #> selectedItem.bind {
      case (trail, Some(item)) =>
        Xml.setContent(item.content(trail))
      case _ => 
        _ => NodeSeq.Empty
    } &
    menuItemSelector #> 
      selectedItem.zipWith(items).bind(_ => NodeSeq.Empty) {
        case ((trail, selected), item) => 
          Xml.addClass(selectedClass, Some(item) == selected) & 
          "a" #> {
            Xml.setText(item.text) & 
            Xml.setAttr("href", item.path.mkString(trail.url.context.toString, "/", "")) & 
            Html.onclick(R.changeUrl(Url(trail.url.context, item.path, Map.empty)))
          }
      }
  }
}
