package net.scalaleafs2

import scala.xml.NodeSeq
import scala.xml.Text

trait Implicits {
  implicit def unparsedCssSelector(s : String) = new UnparsedCssSelector(s)
  
  implicit class XmlLiteral(xml : NodeSeq) extends RenderNode {
    override def apply(context : Context, xml : NodeSeq) : NodeSeq = 
      xml
  }
  implicit class StringLiteral(s : String) extends RenderNode {
    override def apply(context : Context, xml : NodeSeq) : NodeSeq = 
      Text(s)
  }
  
  implicit class RichAny[A](a : => A) {
    def bind(f : => Placeholder[A] => RenderNode) : RenderNode = {
      new RenderNode {
        val values = ArrayBuffer[A]()
        val placeholder = new Placeholder[A](values, 0)
        var child : RenderNode = null
        override def apply(context : Context, xml : NodeSeq) : NodeSeq = {
          placeholder.set(a)
          if (child == null) {
            child = f(placeholder)
          }
          child(context, xml)
        }
      }
    }
  }
  implicit class RichIterable[A](a : => Iterable[A]) {
    def bindAll(f : => Placeholder[A] => RenderNode) : RenderNode = {
      new RenderNode {
        val placeholder = new Placeholder[A]
        var child : RenderNode = null
        override def apply(context : Context, xml : NodeSeq) : NodeSeq = {
          placeholder.set(a.head)
          if (child == null) {
            child = f(placeholder)
          }
          child(context, xml)
        }
      }
    }
  }
  
  implicit class FunctionTransformation(fs : => RenderNode) extends RenderNode {
    override def apply(context : Context, xml : NodeSeq) : NodeSeq = 
      xml
  }
}

package object implicits extends Implicits