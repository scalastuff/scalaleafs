package net.scalaleafs

import scala.xml.NodeSeq

import net.scalaleafs.implicits._
import net.scalaleafs.{XmlTransformation, Var, UrlTrail, Url, SetText, SetContent, SetAttr, R, Html, AddClass}

case class MenuItem(path : List[String], text : String, content : UrlTrail => XmlTransformation) {
  val itemPathString = path.mkString("/")
} 

case class Menu(items : MenuItem*) {
  
  // Longest paths first.
  private val sortedItems = items.sortWith(_.path.size > _.path.size)
  
  def render(trail : Var[UrlTrail], contentSelector : String, menuItemSelector : String, selectedClass : String = "selected") = { 
    val selectedItem = trail map { trail =>
      println("Recalculating url: " + trail)
      (trail, sortedItems.find(item => trail.remainder.startsWith(item.path)))
    }
    contentSelector #> selectedItem.bind {
      case (trail, Some(item)) =>
        SetContent(item.content(trail))
      case _ => 
        _ => NodeSeq.Empty
    } &
    menuItemSelector #> 
      selectedItem.zipWith(items).bind(_ => NodeSeq.Empty) {
        case ((trail, selected), item) => 
          AddClass(selectedClass, Some(item) == selected) & 
          "a" #> {
            SetText(item.text) & 
            SetAttr("href", item.path.mkString(trail.url.context.toString, "/", "")) & 
            Html.onclick(R.changeUrl(Url(trail.url.context, item.path, Map.empty)))
          }
      }
  }
}
