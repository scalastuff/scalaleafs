package org.scalastuff.scalaleafs.contrib
import org.scalastuff.scalaleafs.XmlTransformation
import org.scalastuff.scalaleafs.Url
import scala.xml.NodeSeq

abstract class UrlDispatcher extends XmlTransformation {

  val dispatch : Url => XmlTransformation
  
  override def apply(xml : NodeSeq) : NodeSeq = {
   xml 
  }
}