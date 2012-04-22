package org.scalastuff.scalaleafs.documentation
import org.scalastuff.scalaleafs.JsCmd.toNoop
import org.scalastuff.scalaleafs.implicits._
import org.scalastuff.scalaleafs.AddClass
import org.scalastuff.scalaleafs.HeadContributions
import org.scalastuff.scalaleafs.Ident
import org.scalastuff.scalaleafs.R
import org.scalastuff.scalaleafs.Redrawable
import org.scalastuff.scalaleafs.Template
import org.scalastuff.scalaleafs.Url
import org.scalastuff.scalaleafs.UrlHandler
import org.scalastuff.scalaleafs.Widgets
import org.scalastuff.scalaleafs.CodeSample
import org.scalastuff.scalaleafs.MkElem

class Frame(var url : Url) extends Template with UrlHandler with HeadContributions with CodeSample {
  
  def bind = "#main" #> MkElem("h1")(main)
  
  val main = Redrawable { r =>
//    "#main-menu" #> {
//      "#getting-started a" #> Widgets.onclick(R.url = "getting-started") & 
//      "#templates a" #> Widgets.onclick(R.url = "templates")
//    } & {
    url.remainingPath match {
      case Nil => 
        "#content" #> <h1>Home page</h1>
      case "getting-started" :: Nil =>
        "#getting-started" #> AddClass("selected") & 
        "#content" #> new GettingStarted(url.child) 
      case "templates" :: Nil =>
        "#content" #> new Templates(url.child)
      case "binding" :: rest =>
        "#binding" #> AddClass("open") & 
        "#content" #> new Binding(url.child)
      case _ => Ident
    }}
//  }

  override def handleUrl(url : Url) {
    println("Setting url from " + this.url + " to " + url)
    this.url = url
    main.redraw
  }
}