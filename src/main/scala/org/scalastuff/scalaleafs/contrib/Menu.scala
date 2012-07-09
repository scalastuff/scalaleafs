package org.scalastuff.scalaleafs.contrib

import scala.xml.NodeSeq
import org.scalastuff.scalaleafs.implicits._
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
import org.scalastuff.scalaleafs.UrlTrail

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
    contentSelector #> selectedItem.render {
      case (trail, Some(item)) =>
        SetContent(item.content(trail))
      case _ => 
        _ => NodeSeq.Empty
    } &
    menuItemSelector #> 
      selectedItem.zipWith(items).render(_ => NodeSeq.Empty) {
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
