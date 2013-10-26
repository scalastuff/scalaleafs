package net.scalaleafs2

import scala.xml.NodeSeq
import scala.xml.Text
import scala.collection.mutable.ArrayBuffer

trait Implicits { 
  implicit def unparsedCssSelector(s : String) = new UnparsedCssSelector(s)
  implicit def stringToRenderNode(s : => String) = Xml.replaceWith(s)
  implicit def xmlToRenderNode(xml : => NodeSeq) = Xml.replaceWith(xml)
}

package object implicits extends Implicits