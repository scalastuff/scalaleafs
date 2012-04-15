package org.scalastuff.scalaleafs.documentation

import org.scalastuff.scalaleafs.implicits._
import unfiltered.filter.Planify
import unfiltered.request.Path
import unfiltered.request.Seg
import unfiltered.response.ResponseString
import org.scalastuff.scalaleafs.Configuration
import org.scalastuff.scalaleafs.Url
import org.scalastuff.scalaleafs.leafsFilter
import unfiltered.filter.Plan
import unfiltered.filter.Intent


object SitePlan extends Plan {
  val css = """(.*.css)""".r
  def intent = Intent {
    case Path(Seg("no" :: rest)) => ResponseString("WRONG PAGE")
    case Path(Seg("favicon.ico" :: rest)) => ResponseString("ICON")
    case Path(css(s)) => ResponseString("CSS:" + s)
    case Path(Seg(path)) => 
      ResponseString(new Frame(Url(path)).render.toString())
  }
}

object Site {

  val c = Configuration(debugMode = true)

  def main(args : Array[String]) {
    unfiltered.jetty.Http.local(9001).withleafs(configuration = c).filter(SitePlan).run()
  }
} 

class Site extends Planify(SitePlan.intent) with leafsFilter {
  override val configuration = Configuration(debugMode = true)
}