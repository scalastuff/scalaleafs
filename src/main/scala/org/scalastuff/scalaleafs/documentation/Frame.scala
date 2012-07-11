package net.scalaleafs.documentation

import net.scalaleafs.UrlTrail
import net.scalaleafs.JsCmd.toNoop
import net.scalaleafs.implicits._
import net.scalaleafs.AddClass
import net.scalaleafs.HeadContributions
import net.scalaleafs.Ident
import net.scalaleafs.R
import net.scalaleafs.Template
import net.scalaleafs.Url
import net.scalaleafs.UrlHandler
import net.scalaleafs.Html
import net.scalaleafs.MkElem
import net.scalaleafs.CssSelector
import net.scalaleafs.XmlTransformation
import scala.xml.NodeSeq
import scala.collection.mutable.ArrayBuffer
import net.scalaleafs.SetText
import net.scalaleafs.SetAttr
import net.scalaleafs.Var
import net.scalaleafs.Var
import net.scalaleafs.SeqVar
import net.scalaleafs.SetContent
import net.scalaleafs.Children
import net.scalaleafs.UrlTrail
import net.scalaleafs.Var
import net.scalaleafs.XmlTransformation
import net.scalaleafs.UrlHandler
import net.scalaleafs.Template

class Frame(val trail : Var[UrlTrail]) extends Template with UrlHandler with HeadContributions {
    
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
