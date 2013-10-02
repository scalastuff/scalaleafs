package net.scalaleafs.sample.ui


import net.scalaleafs.JsReturnFalse;
import net.scalaleafs.R;
import net.scalaleafs.UrlTail;
import net.scalaleafs.XmlHelpers;
import net.scalaleafs._
import scala.xml.NodeSeq
import scala.xml.Elem


class Index extends Template {
    def linkup2 = new ElemTransformation { 
    def apply(elem : Elem) = {
      XmlHelpers.attr(elem, "href") match {
        case "" => elem
        case href =>
          XmlHelpers.setAttr(XmlHelpers.setAttr(elem, "href", R.url.context + href), "onclick", (R.callback(_ => R.changeUrl(href)) & JsReturnFalse).toString)
          //XmlHelpers.setAttr(elem, "href", R.url.context + href)
      }          
    }
  }

  
  
  val thetail = Var(R.tail)
  val handler = new UrlHandler {
    val tail = thetail 
  }
  def bind : NodeSeq => NodeSeq = 
  "a" #> linkup2 &
  "#page-content" #> {
    thetail bind {
      case UrlTail("repository", tail) =>
        println("repo matched, tail: " + tail)
        new Books(tail)
      case UrlTail("taxonomy", tail) =>
        println("taxonomy matched, tail: " + tail)
        new Music(tail)
      case _ => 
        println("???")
        xml : NodeSeq => xml
    }
  }
}