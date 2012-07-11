package net.scalaleafs.documentation

import net.scalaleafs.Ident
import net.scalaleafs.UrlHandler
import net.scalaleafs.Url
import net.scalaleafs.Template
import scala.xml.NodeSeq
import net.scalaleafs.UrlTrail

class GettingStarted(val url : UrlTrail) extends Template {
  val path  = Nil
  val bind = { (xml:NodeSeq) =>  xml}
}