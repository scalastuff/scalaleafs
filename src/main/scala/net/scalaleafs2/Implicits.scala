package net.scalaleafs2

import scala.xml.NodeSeq
import scala.xml.Text
import scala.collection.mutable.ArrayBuffer

trait Implicits {
  implicit def unparsedCssSelector(s : String) = new UnparsedCssSelector(s)
  implicit def toRenderNode(s : => String) = Xml.replaceWith(s)
  implicit def toRenderNode(xml : => NodeSeq) = Xml.replaceWith(xml)
  implicit def richAny[A](a : A) = new RichAny[A](a)
  implicit def richOption[A](a : Option[A]) = new RichOption[A](a)
  implicit def richIterable[A](a : Iterable[A]) = new RichIterable(a)
  implicit class FunctionTransformation(fs : => RenderNode) extends RenderNode {
    override def apply(context : Context, xml : NodeSeq) : NodeSeq = 
      xml
  }
}

package object implicits extends Implicits