package org.scalastuff.scalaleafs.documentation

import org.scalastuff.scalaleafs.UrlHandler
import org.scalastuff.scalaleafs.Url
import org.scalastuff.scalaleafs.Template
import org.scalastuff.scalaleafs.implicits._
import scala.xml.NodeSeq

class Templates(val url : Url) extends Template {
  val path  = Nil
  val bind = 
    "#sample1" #> new Sample1 & 
    "#sample2" #> new Sample2
}