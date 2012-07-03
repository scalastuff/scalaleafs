package org.scalastuff.scalaleafs
import scala.xml.NodeSeq
import implicits._
import scala.xml.Elem
import java.util.UUID
import com.sun.jmx.mbeanserver.WeakIdentityHashMap
import scala.collection.mutable.WeakHashMap
//
//object Redrawable {
//  def apply(f : Redrawable => NodeSeq => NodeSeq) = {
//    new Redrawable()(f)
//  }
//  def apply(f : XmlTransformation) = {
//    new Redrawable()(_ => f)
//  }
//}
//
//class Redrawable extends XmlTransformation {
//  private lazy val generatedId = UUID.randomUUID().toString()
//  private var _id : String = ""
//  private var _input : NodeSeq = NodeSeq.Empty
//  private var _transform : Redrawable => NodeSeq => NodeSeq = r => Ident
//  def id : String = _id
//  override def apply(xml: NodeSeq) = xml match {
//    case ElemWithId(elem, id) => 
//      _id = id
//      _input = xml
//      _transform(this)(_input)
//    case elem : Elem =>
//      _id = generatedId
//      _input = elem.setAttr("id", _id)
//      _transform(this)(_input)
//    case _ => xml
//  }
//  def redraw {
//    val xml = _transform(this)(_input)
//    R.addPostRequestJs(ReplaceHtml(_id, xml))
//  }
//  def apply(f : Redrawable => NodeSeq => NodeSeq) : Redrawable = {
//    _transform = f
//    this
//  }
//}
