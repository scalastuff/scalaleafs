package org.scalastuff.scalaleafs.documentation

import org.scalastuff.scalaleafs.implicits._
import unfiltered.filter.Planify
import unfiltered.request.Path
import unfiltered.request.Seg
import unfiltered.response.ResponseString
import org.scalastuff.scalaleafs.Configuration
import org.scalastuff.scalaleafs.Url
import org.scalastuff.scalaleafs.LeafsFilter
import unfiltered.filter.Plan
import unfiltered.filter.Intent
import org.scalastuff.scalaleafs.contrib.SyntaxHighlighterUrl
import unfiltered.filter.Intent
import org.scalastuff.scalaleafs.Configuration
import org.scalastuff.scalaleafs.DebugMode
import org.scalastuff.scalaleafs.Var
import org.scalastuff.scalaleafs.UrlTrail

object Site {

  val c = new Configuration (
    DebugMode -> true,
    SyntaxHighlighterUrl -> "true" 
  )
  
  val css = """(.*.css)""".r

  val intent = Intent {
    case Path(Seg("no" :: rest)) => ResponseString("WRONG PAGE")
    case Path(Seg("favicon.ico" :: rest)) => ResponseString("ICON")
    case Path(css(s)) => ResponseString("CSS:" + s)
//    case Path(Seg(path)) => 
//      ResponseString(new Frame(Var(Url(path))).render.toString())
  }
  
  def main(args : Array[String]) {
//    unfiltered.jetty.Http.local(9001).withleafs(c).filter(Planify(intent)).run()
  }
} 

class DocumentationFilter extends LeafsFilter {
  val configuration = new Configuration()
  def render(trail : UrlTrail) = new Frame(trail).render
  
}