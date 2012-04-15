package org.scalastuff.scalaleafs.documentation

import org.scalastuff.scalaleafs.Ident
import org.scalastuff.scalaleafs.UrlHandler
import org.scalastuff.scalaleafs.Url
import org.scalastuff.scalaleafs.Template
import scala.xml.NodeSeq

class Templates(val url : Url) extends Template {
  val path  = Nil
  val bind = { (xml:NodeSeq) => println(xml); xml}
}