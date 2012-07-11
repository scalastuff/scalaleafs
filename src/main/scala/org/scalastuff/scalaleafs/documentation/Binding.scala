package net.scalaleafs.documentation
import scala.xml.NodeSeq

import net.scalaleafs.Template
import net.scalaleafs.Url

class Binding(url : Url) extends Template {
  val bind = (xml : NodeSeq) => <bla/>
}