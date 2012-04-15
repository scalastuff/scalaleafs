package org.scalastuff.scalaleafs.documentation
import scala.xml.NodeSeq

import org.scalastuff.scalaleafs.Template
import org.scalastuff.scalaleafs.Url

class Binding(url : Url) extends Template {
  val bind = (xml : NodeSeq) => <bla/>
}