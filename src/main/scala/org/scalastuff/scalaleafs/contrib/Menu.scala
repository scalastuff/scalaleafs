package org.scalastuff.scalaleafs.contrib

import scala.xml.NodeSeq
import org.scalastuff.scalaleafs.JsCmd.toNoop
import org.scalastuff.scalaleafs.R.toTransientRequest
import org.scalastuff.scalaleafs.implicits.toUnparsedCssSelector
import org.scalastuff.scalaleafs.{XmlTransformation, Widgets, Var, Url, SetText, SetAttr, R, Ident, AddClass, CssTransformation}
import org.scalastuff.scalaleafs.SetContent

case class MenuItem(path : List[String], text : String, content : Url => XmlTransformation) {
  val itemPathString = path.mkString("/")
} 

case class Menu(items : MenuItem*) {
  
  // Longest paths first.
  private val sortedItems = items.sortWith(_.path.size > _.path.size)
  
  def bind(url : Var[Url], contentSelector : String, menuItemSelector : String, selectedClass : String = "selected") = { 
    val selectedItem = url map { url =>
      println("Recalculating url: " + url)
      (url, sortedItems.find(item => url.remainingPath.startsWith(item.path)))
    }
    contentSelector #> selectedItem.bind {
      case (url, Some(item)) =>
        SetContent(item.content(url))
      case _ => 
        _ => NodeSeq.Empty
    } &
    menuItemSelector #> 
      selectedItem.zipWith(items).bind(_ => NodeSeq.Empty) {
        case ((url, selected), item) => 
          AddClass(selectedClass, Some(item) == selected) & 
          "a" #> {
            SetText(item.text) & 
            SetAttr("href", item.path.mkString(url.context.toString, "/", "")) & 
            Widgets.onclick(R.url = item.path.mkString("/"))
          }
      }
  }
}
