package org.scalastuff.scalaleafs
import scala.xml.NodeSeq
import implicits._
import scala.xml.Elem
import java.util.UUID
import com.sun.jmx.mbeanserver.WeakIdentityHashMap
import java.util.WeakHashMap

object Redrawable {
  def apply(f : Redrawable => NodeSeq => NodeSeq) = {
    new Redrawable()(f)
  }
}

class Redrawable extends XmlTransformation {
  private lazy val generatedId = UUID.randomUUID().toString()
  private var _id : String = ""
  private var _input : NodeSeq = NodeSeq.Empty
  private var _transform : Redrawable => NodeSeq => NodeSeq = r => Ident
  def id : String = _id
  def apply(xml: NodeSeq) = xml match {
    case ElemWithId(elem, id) => 
      _id = id
      _input = xml
      _transform(this)(xml)
    case elem : Elem =>
      _id = generatedId
      _input = elem.setAttr("id", _id)
      _transform(this)(xml)
    case _ => xml
  }
  def redraw {
    val xml = _transform(this)(_input)
    R.addPostRequestJs(ReplaceHtml(_id, xml))
  }
  def apply(f : Redrawable => NodeSeq => NodeSeq) : Redrawable = {
    _transform = f
    this
  }
}

trait ChangeableListener

object Changeable {
  implicit def toValue[A](changeable : Changeable[A]) = changeable.value
}

trait Changeable[A] {
  protected var _currentValue : A
  private val listeners = new WeakHashMap[ChangeableListener, ChangeableListener]
  private[scalaleafs] def addListener(listener : ChangeableListener)
  protected def notifyChanged {}
  protected def setValue(value : A) {
    if (value != _currentValue) {
      notifyChanged
      _currentValue = value
    }
    
  }
  def value : A = _currentValue
  def map[B](f : A => B) : Changeable[B] = {
    null
  }
}

abstract class Var[A](initialValue : A) extends Changeable[A] {
  private var _value = initialValue
}

abstract class MappedChangeable[A, B](original : Changeable[A], f : A => B) extends Changeable[B] with ChangeableListener {
  original.addListener(this)
  setValue(f(original))
}