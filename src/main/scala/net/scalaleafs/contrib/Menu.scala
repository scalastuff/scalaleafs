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

case class MenuItem(path : List[String], text : String, content : UrlTail => XmlTransformation) {
  val itemPathString = path.mkString("/")
} 

case class Menu(items : MenuItem*) {
  
  // Longest paths first.
  private val sortedItems = items.sortWith(_.path.size > _.path.size)
  
  def render(tail : Var[UrlTail], contentSelector : String, menuItemSelector : String, selectedClass : String = "selected") = { 
    val selectedItem = tail map { tail =>
      (tail, sortedItems.find(item => tail.remainingPath.startsWith(item.path)))
    }
    contentSelector #> selectedItem.bind {
      case (tail, Some(item)) =>
        Xml.setContent(item.content(tail))
      case _ => 
        _ => NodeSeq.Empty
    } &
    menuItemSelector #> 
      selectedItem.zipWith(items).bind(_ => NodeSeq.Empty) {
        case ((tail, selected), item) => 
          Xml.addClass(selectedClass, Some(item) == selected) & 
          "a" #> {
            Xml.setText(item.text) & 
            Xml.setAttr("href", item.path.mkString(tail.context.toString, "/", "")) & 
            Html.onclick(R.changeUrl(Url(tail.context, item.path, Map.empty)))
          }
      }
  }
}
