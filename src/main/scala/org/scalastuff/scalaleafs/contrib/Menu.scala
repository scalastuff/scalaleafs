package org.scalastuff.scalaleafs.contrib

import scala.xml.NodeSeq

import org.scalastuff.scalaleafs.JsCmd.toNoop
import org.scalastuff.scalaleafs.R.toTransientRequest
import org.scalastuff.scalaleafs.implicits.toUnparsedCssSelector
import org.scalastuff.scalaleafs.AddClass
import org.scalastuff.scalaleafs.CssTransformation
import org.scalastuff.scalaleafs.Html
import org.scalastuff.scalaleafs.R
import org.scalastuff.scalaleafs.SetAttr
import org.scalastuff.scalaleafs.SetContent
import org.scalastuff.scalaleafs.SetText
import org.scalastuff.scalaleafs.Url
import org.scalastuff.scalaleafs.Var
import org.scalastuff.scalaleafs.XmlTransformation

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
            Html.onclick(R.changeUrl(Url(url.context, url.currentPath, item.path, Map.empty)))
          }
      }
  }
}
