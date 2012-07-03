package org.scalastuff.scalaleafs

import scala.xml.NodeSeq
import scala.xml.Elem
import scala.xml.Elem$
import scala.xml.TopScope
import scala.xml.MetaData
import scala.xml.Node
import java.util.UUID

trait XmlTransformation extends Function1[NodeSeq, NodeSeq] { t0 =>
  
  /**
   * By default, perform identity transformation. 
   */
  def apply(xml : NodeSeq) : NodeSeq = xml
  
  /**
   * Nested transformations are applied before this transformation.
   */
  def apply(t : XmlTransformation) : XmlTransformation = new XmlTransformation {
    override def apply(xml : NodeSeq) : NodeSeq = t0(t(xml))
  }
  
  /**
   * Concatenated transformations are applied after this transformation.
   */
  def & (t : XmlTransformation) : XmlTransformation = new XmlTransformation {
    override def apply(xml : NodeSeq) : NodeSeq = t(t0(xml))
  }
}

case class XmlSelector(matches : Elem => Boolean, recursive : Boolean = false, nested : Option[XmlSelector] = None) 

object XmlTransformation {
  def apply(selector : XmlSelector, replace : Elem => NodeSeq) = new XmlTransformation {
    override def apply(xml : NodeSeq) = 
      transform(xml, selector, replace)
  } 
  def transform(xml : NodeSeq, matches : Elem => Boolean, replace : Elem => NodeSeq) : NodeSeq =
    transform(xml, XmlSelector(matches), replace)
    
  def transform(xml : NodeSeq, selector : XmlSelector, replace : Elem => NodeSeq) : NodeSeq = {
    var changed = false
    var builder = NodeSeq.newBuilder
    xml foreach {
      case elem : Elem if selector.matches(elem) => 
        selector.nested match {
          case Some(nested) =>
            val recursed = transform(elem.child, nested, replace)
            // Optimization: Don't clone when child list is the same instance
            if (!(recursed eq elem.child)) {
              changed = true;
              builder += elem.copy(child = recursed)
            } else {
              builder += elem
            }
          case None =>
            val replacement = replace(elem)
            builder ++= replacement
            changed = true
        }
      case elem : Elem if selector.recursive => 
        val recursed = transform(elem.child, selector, replace)
        // Optimization: Don't clone when child list is the same instance
        if (!(recursed eq elem.child)) {
          changed = true;
          builder += elem.copy(child = recursed)
        } else {
          builder += elem
        }
      case node => 
        builder += node
    }
    // Optimization: Make sure the same node list is returned when nothing changed.
    if (changed) builder.result
    else xml
  }
}


/**
 * XmlTransformation delegate. Useful for classes that express a transformation
 * using CssTransformations.
 */
trait HasXmlTransformation extends XmlTransformation {
  override def apply(xml : NodeSeq) = transformation.apply(super.apply(xml))
  def transformation : XmlTransformation 
}

/**
 * Transformation that transforms some input XML with each of the given transformations and concatenates the result. 
 * Used to loop over a sequence that is mapped onto XML transformation. An implicit conversion will
 * create an SeqXmlTransformation instance from a sequence of transformations.    
 */
class SeqXmlTransformation(transformations : Seq[NodeSeq => NodeSeq]) extends XmlTransformation {
  override def apply(xml : NodeSeq) = transformations.map(_(xml)).flatten
}

/**
 * Transformation that needs an Elem as input. If the elem is not an elem, the transformation is a no-op.
 */
trait ElemTransformation extends XmlTransformation with Function1[Elem, NodeSeq] {
  override def apply(xml : NodeSeq) : NodeSeq = xml match {
    case elem : Elem => apply(elem)
    case Seq(elem : Elem) => apply(elem)
    case xml => xml
  }
  def apply(elem : Elem) : NodeSeq
}

trait ElemWithIdTransformation extends ElemTransformation {
  lazy val generatedId = UUID.randomUUID().toString()
  def apply(elem : Elem) : NodeSeq = 
    XmlHelpers.getId(elem).trim match {
      case "" => apply(XmlHelpers.setId(elem, generatedId), generatedId)
      case id => apply(elem, id)
    }
  def apply(elem : Elem, id : String) : NodeSeq
}

/**
 * Transformation that transforms an Elem into another Elem. A condition function can be specified that
 * when false, will turn the transformation into a no-op. This is useful for conditional element modification,
 * like adding a class attribute if some condition is met.
 */
class ElemModifier(modify : Elem => Elem, condition : => Boolean) extends ElemTransformation {
  def &(modifier : ElemModifier) : ElemModifier = new ElemModifier(e => modifier(this(e)), true) 
  def apply(elem : Elem) : Elem = if (condition) modify(elem) else elem
}

/**
 * Identity transformation.
 */
object Ident extends ElemModifier(e => e, false) with XmlTransformation {
  override def & (modifier : ElemModifier) : ElemModifier = modifier 
  override def & (t : XmlTransformation) : XmlTransformation = t
}

/**************************
 * Utility transformations.
 **************************/

object MkElem {
  def apply(cssConstructor : String) : MkElem =  
    new MkElem(cssConstructor, true, Ident) 

  def apply(cssConstructor : String, condition : => Boolean) : MkElem = 
    new MkElem(cssConstructor, condition, Ident) 

  def apply(cssConstructor : String, modifier : ElemModifier) : MkElem = 
    new MkElem(cssConstructor, true, modifier) 

  def apply(cssConstructor : String, condition : => Boolean, modifier : ElemModifier) : MkElem = 
    new MkElem(cssConstructor, condition, modifier) 
}

class MkElem(cssConstructor : String, condition : => Boolean, modifier : ElemModifier) extends XmlTransformation {
  override def apply(xml : NodeSeq) : NodeSeq = {
    if (condition) modifier(CssConstructor.getOrParse(cssConstructor)(xml)) else xml
  }
}

object ReplaceElem {
  def apply(cssConstructor : String) : ElemModifier =  
    new ElemModifier(e => CssConstructor.getOrParse(cssConstructor)(e), true) 

  def apply(cssConstructor : String, condition : => Boolean) : ElemModifier = 
    new ElemModifier(e => CssConstructor.getOrParse(cssConstructor)(e), condition) 

  def apply(cssConstructor : String, modifier : ElemModifier) : ElemModifier = 
    new ElemModifier(e => modifier(CssConstructor.getOrParse(cssConstructor)(e)), true) 

  def apply(cssConstructor : String, condition : => Boolean, modifier : ElemModifier) : ElemModifier = 
    new ElemModifier(e => modifier(CssConstructor.getOrParse(cssConstructor)(e)), condition) 
}

object Children {
  def apply(condition : => Boolean = true) : ElemTransformation = new ElemTransformation {
    override def apply(elem : Elem) : NodeSeq = 
      if (condition) elem.child
      else elem
  }
}

object SetAttr {
  def apply(key : String, value : String, condition : => Boolean = true) = 
    new ElemModifier(XmlHelpers.setAttr(_, key, value), condition) 
}

object RemoveAttr {
  def apply(key : String, value : String, condition : => Boolean = true) = 
    new ElemModifier(XmlHelpers.removeAttr(_, key), condition) 
}

object AddAttrValue {
  def apply(key : String, value : String, condition : => Boolean = true) = 
    new ElemModifier(XmlHelpers.addAttrValue(_, key, value), condition) 
}

object RemoveAttrValue {
  def apply(key : String, value : String, condition : => Boolean = true) = 
    new ElemModifier(XmlHelpers.removeAttrValue(_, key, value), condition) 
}

object SetId {
  def apply(value : String, condition : => Boolean = true) = 
    new ElemModifier(XmlHelpers.setId(_, value), condition) 
}

object SetClass {
  def apply(value : String, condition : => Boolean = true) = 
    new ElemModifier(XmlHelpers.setClass(_, value), condition) 
}

object AddClass {
  def apply(value : String, condition : => Boolean = true) = 
    new ElemModifier(XmlHelpers.addClass(_, value), condition) 
}

object RemoveClass {
  def apply(value : String, condition : => Boolean = true) = 
    new ElemModifier(XmlHelpers.removeClass(_, value), condition) 
}

object SetText {
  def apply(value : String, condition : => Boolean = true) = 
    new ElemModifier(XmlHelpers.setText(_, value), condition) 
}

object SetContent {
  def apply(content : NodeSeq => NodeSeq, condition : => Boolean = true) = 
    new ElemModifier(xml => XmlHelpers.setContent(xml, content(xml.child)), condition) 
}