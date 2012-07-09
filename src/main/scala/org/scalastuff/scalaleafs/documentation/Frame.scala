package org.scalastuff.scalaleafs.documentation

import org.scalastuff.scalaleafs.JsCmd.toNoop
import org.scalastuff.scalaleafs.implicits._
import org.scalastuff.scalaleafs.AddClass
import org.scalastuff.scalaleafs.HeadContributions
import org.scalastuff.scalaleafs.Ident
import org.scalastuff.scalaleafs.R
import org.scalastuff.scalaleafs.Template
import org.scalastuff.scalaleafs.Url
import org.scalastuff.scalaleafs.UrlHandler
import org.scalastuff.scalaleafs.Html
import org.scalastuff.scalaleafs.CodeSample
import org.scalastuff.scalaleafs.MkElem
import org.scalastuff.scalaleafs.CssSelector
import org.scalastuff.scalaleafs.XmlTransformation
import scala.xml.NodeSeq
import scala.collection.mutable.ArrayBuffer
import org.scalastuff.scalaleafs.contrib.UrlDispatcher
import org.scalastuff.scalaleafs.SetText
import org.scalastuff.scalaleafs.SetAttr
import org.scalastuff.scalaleafs.Var
import org.scalastuff.scalaleafs.Var
import org.scalastuff.scalaleafs.SeqVar
import org.scalastuff.scalaleafs.SetContent
import org.scalastuff.scalaleafs.Children
import org.scalastuff.scalaleafs.UrlTrail

class Frame(val trail : Var[UrlTrail]) extends Template with UrlHandler with HeadContributions with CodeSample {
    
  case class MenuItem(path : List[String], text : String, f : UrlTrail => XmlTransformation) 
  val menu = Seq(
    MenuItem("getting-started" :: Nil, "Getting Started", url => new GettingStarted(url.advance)),
    MenuItem("templates" :: Nil, "Templates", url => new Templates(url.advance))
  )

  val bindMenu = "#menu-item" #> 
   trail.zipWith(menu).render(_ => NodeSeq.Empty) { 
     case (url, item) =>
        AddClass("selected", url.remainder.startsWith(item.path)) & 
        "a" #> {
          SetText(item.text) & 
          SetAttr("href", item.path.mkString("/")) & 
          Html.onclick(R.changeUrl(item.path.mkString("/")))
      }
    }  
    
  val bindContent = "#content" #> 
    trail.render { url =>
      menu.find(i => url.remainder.startsWith(i.path)) match {
        case Some(item) => SetContent(item.f(url))
        case None  => Ident
      }
    }

   val bind = bindMenu & bindContent
}
