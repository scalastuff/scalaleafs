package net.scalaleafs.documentation

import net.scalaleafs.UrlHandler
import net.scalaleafs.Url
import net.scalaleafs.Template
import net.scalaleafs.implicits._
import scala.xml.NodeSeq
import net.scalaleafs.UrlTrail

class Templates(val url : UrlTrail) extends Template {
  val path  = Nil
  val bind = 
    "#sample1" #> new Sample1 & 
    "#sample2" #> new Sample2
}