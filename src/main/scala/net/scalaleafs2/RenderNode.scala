package net.scalaleafs2

import scala.xml.Elem
import scala.xml.NodeSeq
import java.util.UUID

/**
 * An xml transformation is a composable NodeSeq => NodeSeq function. 
 * Transformations are composed using apply(Transformation) and &(Transformation).
 * The Transformation trait is used for many operations in ScalaLeafs.
 */
trait RenderNode {

  def children : Iterable[RenderNode]

  def render(context : Context, xml : NodeSeq) : NodeSeq
  
  def renderChanges(context : Context) {
    children.foreach(_.renderChanges(context))
  }
  
  def dispose(context : Context) {
    children.foreach(_.dispose(context))
  }

  /**
   * Compose with a nested transformation.
   * Nested transformations are applied before this transformation.
   */
  def apply(node : RenderNode) : RenderNode = 
    new CompoundRenderNode(Seq(node, this))
  
  /**
   * Compose with a concatenated transformation.
   * Concatenated transformations are applied after this transformation.
   */
  def & (node : RenderNode) : RenderNode =
    new CompoundRenderNode(Seq(this, node))
}

trait ElemRenderNode extends RenderNode {
  override def render(context : Context, xml : NodeSeq) : Elem
}

trait RenderLeaf extends RenderNode {
  def children = Nil
}

object IdentRenderNode extends ElemModifier((_, elem) => elem, false) {
  override def render(context : Context, xml : NodeSeq) = xml
  override def & (modifier : ElemModifier) = modifier 
  override def & (node : RenderNode) = node 
  override def apply (modifier : ElemModifier) = modifier
  override def apply (node : RenderNode) = node
}

final class CompoundRenderNode(val children: Iterable[RenderNode]) extends RenderNode {
  def render(context : Context, xml : NodeSeq) = 
    children.foldLeft(xml)((xml, child) => child.render(context, xml))
    
  override def apply(node : RenderNode) : RenderNode = 
    new CompoundRenderNode(Seq(node) ++ children)

  override def & (node : RenderNode) : RenderNode =
    new CompoundRenderNode(children ++ Seq(node))
}

/**
 * Render node delegate. Useful for classes that express a render node
 * using CssTransformations, for example.
 */
trait HasRenderNode extends RenderNode {
  def delegate : RenderNode

  def render(context : Context, xml : NodeSeq) = 
    delegate.render(context, xml)
    
  def children = 
    delegate.children
}

/**
 * Render node that expects an Elem as input. 
 * If the elem is not an elem, the identity transformation is used.
 * Implementations should override render(context, elem).
 */
trait ExpectElemRenderNode extends RenderNode {
  def render(context : Context, xml : NodeSeq) : NodeSeq = xml match {
    case elem : Elem => render(context, elem)
    case Seq(elem : Elem) => render(context, elem)
    case xml => xml
  }
  def render(context : Context, elem : Elem) : NodeSeq
}

/**
 * Render node that ensures that some element has an id. 
 * If the input element doesn't have an
 * id, the output element will contain a generated UUID id. 
 * Implementations should override render(context, elem, id).
 */
trait ExpectElemWithIdRenderNode extends ExpectElemRenderNode {
  lazy val generatedId = UUID.randomUUID().toString()
  def render(context : Context, elem : Elem) : NodeSeq = 
    XmlHelpers.getId(elem).trim match {
      case "" => render(context, XmlHelpers.setId(elem, generatedId), generatedId)
      case id => render(context, elem, id)
    }
  def render(context : Context, elem : Elem, id : String) : NodeSeq
}

/**
 * Render node that transforms an Elem into another Elem. 
 * A condition function can be specified that, when false, will use identity transformation. 
 * This is useful for conditional element modification,
 * like adding a class attribute if some condition is met.
 */
class ElemModifier(modify : (Context, Elem) => Elem, condition : => Boolean) extends ExpectElemRenderNode with ElemRenderNode with RenderLeaf  {
  def &(modifier : ElemModifier) : ElemRenderNode = 
    new CompoundElemModifier(Seq(this, modifier))
  
  def apply(modifier : ElemModifier) : ElemRenderNode = 
    new CompoundElemModifier(Seq(modifier, this))
  
  def render(context : Context, elem : Elem) : Elem = 
    if (condition) modify(context, elem) 
    else elem
}

final class CompoundElemModifier(val children : Iterable[ElemModifier]) extends ElemRenderNode {
  def render(context : Context, elem : Elem) : Elem =
    children.foldLeft(elem)((elem, child) => child.render(context, elem))
}


