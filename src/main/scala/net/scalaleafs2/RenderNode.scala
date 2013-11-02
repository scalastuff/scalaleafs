package net.scalaleafs2

import scala.xml.Elem
import scala.xml.NodeSeq
import java.util.UUID
import scala.concurrent.Future
import scala.annotation.tailrec

/**
 * A render node is in essence a NodeSeq => NodeSeq transformation. Render-nodes
 * form a render tree, which is persisted per window. Each rendering can cause
 * the render tree to change.  
 */
trait RenderNode {

  def render(context : Context, xml : NodeSeq) : NodeSeq
  
  def renderChanges(context : Context) : JSCmd
  
  def show(context : Context) : Unit
  
  def hide(context : Context) : Unit
  
  def dispose(context : Context) : Unit
  
  /**
   * Compose with a nested transformation.
   * Nested transformations are applied before this transformation.
   */
  def apply(node : RenderNode) : RenderNode =
    new CompoundRenderNode(node :: this :: Nil)
  
  /**
   * Compose with a concatenated transformation.
   * Concatenated transformations are applied after this transformation.
   */
  def & (node : RenderNode) : RenderNode =
    new CompoundRenderNode(this :: node :: Nil)  
}

trait NoChildRenderNode {
  def dispose(context : Context) : Unit = Unit
  def show(context : Context) : Unit = Unit
  def hide(context : Context) : Unit = Unit
}

trait SingleChildRenderNode {
  def child : RenderNode
  def dispose(context : Context) = child.dispose(context)
  def show(context : Context) = child.show(context)
  def hide(context : Context) = child.hide(context)
}

trait MultiChildrenRenderNode {
  def children : Iterable[_ <: RenderNode]
  def dispose(context : Context) = children.foreach(_.dispose(context))
  def show(context : Context) = children.foreach(_.show(context))
  def hide(context : Context) = children.foreach(_.hide(context))
}


final class CompoundRenderNode(val children: List[RenderNode]) extends RenderNode with MultiChildrenRenderNode {
  def render(context : Context, xml : NodeSeq) =
    children.foldLeft(xml)((xml, node) => node.render(context, xml))

  def renderChanges(context : Context) =
    children.foldLeft[JSCmd](JSNoop)((cmd, node) => node.renderChanges(context))
    
  override def apply(node : RenderNode) : RenderNode = 
    new CompoundRenderNode(node :: children)

  override def & (node : RenderNode) : RenderNode =
    new CompoundRenderNode(children ++ List(node))
}

object IdentRenderNode extends ElemModifier with NoChildRenderNode {
  def render(context : Context, elem : Elem) = elem
  def renderChanges(context: net.scalaleafs2.Context): net.scalaleafs2.JSCmd = JSNoop
  override def & (node : RenderNode) = node 
  override def apply (node : RenderNode) = node
  val modify = (context : Context, elem : Elem) => elem  
}

/**
 * Render node delegate. Useful for classes that express a render node
 * using CssTransformations, for example.
 */
trait HasRenderNode extends RenderNode {
  def delegate : RenderNode

  def render(context : Context, xml : NodeSeq) = 
    delegate.render(context, xml)

  def renderChanges(context : Context) = 
    delegate.renderChanges(context)
    
  def dispose(context : Context) =
    delegate.dispose(context)
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
trait ElemModifier extends ExpectElemRenderNode {
  val modify : (Context, Elem) => Elem
  def render(context : Context, elem : Elem) : Elem 
}

final class ConditionalElemModifier(val modify : (Context, Elem) => Elem, condition : => Boolean) extends ElemModifier with NoChildRenderNode {
  
  def render(context : Context, elem : Elem) : Elem = 
    if (condition) modify(context, elem) 
    else elem
    
  def renderChanges(context : Context) = JSNoop
}

