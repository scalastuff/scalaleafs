package net.scalaleafs2

import implicits._
import scala.xml.NodeSeq
import scala.collection.mutable.ArrayBuffer
import scala.xml.Elem

object Placeholder {
  implicit def toA[A](e : Placeholder[A]) : A = e.get
}

class Placeholder[A](initialValue : A) {
  private[scalaleafs2] var value : A = initialValue
  def get : A = value
}

class BoundAny[A, B](getValues : => Iterable[B], f : => Placeholder[B] => RenderNode) extends ExpectElemWithIdRenderNode {
  var lastElem : Elem = null
  var lastId : String = null
  var maxSize : Int = 0
  val placeholders = ArrayBuffer[Placeholder[B]]()
  val children = ArrayBuffer[RenderNode]()
  def render(context : Context, elem : Elem, id : String) : NodeSeq = {
    lastElem = elem
    lastId = id
    val values = getValues.toSeq
    placeholders.dropRight(placeholders.size - values.size)
    children.dropRight(children.size - values.size)
    while (children.size < values.size)  {
      val index = children.size
      val placeholder = new Placeholder[B](values(index)) 
      placeholders += placeholder
      children += f(placeholder)
    }
    if (maxSize < children.size)
      maxSize = children.size
    children.zipWithIndex.flatMap {
      case (child, index) =>
        placeholders(index).value = values(index)
        child.render(context, 
          if (index == 0) elem 
          else XmlHelpers.setId(elem, id + "-" + index)) 
    } 
  }
}

class RichAny[A](val any : A) extends AnyVal {
  def bind(f : => Placeholder[A] => RenderNode) = 
    new BoundAny[A, A](Iterable(any), f)
}

class RichOption[A](val any : Option[A]) extends AnyVal {
  def bindAll(f : => Placeholder[A] => RenderNode) = 
    new BoundAny(any.toIterable, f)
}

class RichIterable[A](val any : Iterable[A]) extends AnyVal {
  def bindAll(f : => Placeholder[A] => RenderNode) = 
    new BoundAny(any, f)
}
