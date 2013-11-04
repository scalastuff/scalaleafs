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
  
  def when(condition : => Boolean) = 
    new RenderNodeDelegate(if (condition) this else Ident)
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
    children.foldLeft[JSCmd](Noop)((cmd, node) => cmd & node.renderChanges(context))
    
  override def apply(node : RenderNode) : RenderNode = 
    new CompoundRenderNode(node :: children)

  override def & (node : RenderNode) : RenderNode =
    new CompoundRenderNode(children ++ List(node))
}

object Ident extends ElemModifier with NoChildRenderNode {
  def render(context : Context, elem : Elem) = elem
  def renderChanges(context: net.scalaleafs2.Context): net.scalaleafs2.JSCmd = Noop
  override def & (node : RenderNode) = node 
  override def apply (node : RenderNode) = node
  val modify = (context : Context, elem : Elem) => elem  
}

/**
 * Render node delegate. Useful for classes that express a render node
 * using CssTransformations, for example.
 */
class RenderNodeDelegate(delegate : => RenderNode) extends RenderNode with SingleChildRenderNode {

  def child = delegate 
  
  def render(context : Context, xml : NodeSeq) = 
    delegate.render(context, xml)

  def renderChanges(context : Context) = 
    delegate.renderChanges(context)
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
    case xml => render(context, <dummy>{xml}</dummy>) match {
      case elem : Elem if elem.label == "dummy" => elem.child
      case xml => xml
    }
  }
  def render(context : Context, elem : Elem) : NodeSeq
}


/**
 * Render node that ensures that some element has an id. 
 * If the input element doesn't have an
 * id, the output element will contain a generated UUID id. 
 * Implementations should override render(context, elem, id).
 */
trait ExpectElemWithIdRenderNode extends RenderNode {
  private lazy val generatedId = UUID.randomUUID().toString()
  private var id : String = null
  def render(context : Context, xml : NodeSeq) : NodeSeq = xml match {
    case elem : Elem => postProcess(render(context, elem))
    case Seq(elem : Elem) => postProcess(render(context, elem))
    case xml => postProcess(render(context, <dummy>{xml}</dummy>) match {
      case elem : Elem if elem.label == "dummy" => elem.child
      case xml => xml
    })
  }
  private def render(context : Context, elem : Elem) : NodeSeq = {
    XmlHelpers.getId(elem).trim match {
      case "" =>
        id = generatedId
        render(context, XmlHelpers.setId(elem, id), id)
      case id =>
        this.id = id
        render(context, elem, id)
    }
  }
  protected def postProcess(xml : NodeSeq) = xml match {
    case elem : Elem => XmlHelpers.setId(elem, id)
    case Seq(elem : Elem) => XmlHelpers.setId(elem, id)
    case xml => <dummy id={id}>xml</dummy>
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
  def render(context : Context, elem : Elem) : Elem 
}

object ElemModifier {
  def apply(modify : (Context, Elem) => Elem) = 
     new ElemModifier with NoChildRenderNode {
      def render(context : Context, elem : Elem) : Elem = 
        modify(context, elem)
      def renderChanges(context : Context) = Noop
    }
}

