package org.scalastuff.scalaleafs.sample
import org.scalastuff.scalaleafs.Configuration
import org.scalastuff.scalaleafs.DebugMode
import unfiltered.filter.Intent
import unfiltered.request.Path
import unfiltered.request.Seg
import unfiltered.response.ResponseString
import org.scalastuff.scalaleafs.Url

object SampleApp {

  val c = new Configuration(
    DebugMode -> true
  )

    
  val css = """(.*.css)""".r

  val intent = Intent {
    case Path(Seg("no" :: rest)) => ResponseString("WRONG PAGE")
    case Path(Seg("favicon.ico" :: rest)) => ResponseString("ICON")
    case Path(css(s)) => ResponseString("CSS:" + s)
//    case Path(Seg(path)) => 
//      ResponseString(new SampleFrame(Url(path)).render.toString())
  }
  
  def main(args : Array[String]) {
//    unfiltered.jetty.Http.local(9001).withleafs(c).filter(Planify(intent)).run()
  }

}