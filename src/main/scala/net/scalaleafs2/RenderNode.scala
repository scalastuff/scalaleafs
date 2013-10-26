package net.scalaleafs2

import scala.xml.Elem
import scala.xml.NodeSeq
import java.util.UUID
import scala.concurrent.Future
import scala.annotation.tailrec

/**
 * An xml transformation is a composable NodeSeq => NodeSeq function. 
 * Transformations are composed using apply(Transformation) and &(Transformation).
 * The Transformation trait is used for many operations in ScalaLeafs.
 */
trait RenderNode {

  def renderAsync(context : Context, xml : NodeSeq) : Future[NodeSeq]
  
  def renderChangesAsync(context : Context) : Future[JSCmd]
  
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

object RenderNode {
  
  def renderAsync(context : Context, xml : NodeSeq, nodes : List[RenderNode]) : Future[NodeSeq] =  
    nodes match {
      case Nil => Future.successful(xml)
      case node :: Nil => node.renderAsync(context, xml)
      case node :: rest => renderAsync(context, xml, rest).flatMap(xml => node.renderAsync(context, xml))(context.executionContext)
    }
  
  def renderChangesAsync(context : Context, nodes : List[RenderNode]) : Future[JSCmd] =  
    nodes match {
      case Nil => Future.successful(JsNoop)
      case node :: Nil => node.renderChangesAsync(context)
      case nodes => Future.fold(nodes.map(_.renderChangesAsync(context)))(JsNoop.asInstanceOf[JSCmd])(_ & _)(context.executionContext)
    }  
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
  def renderAsync(context : Context, xml : NodeSeq) = 
    RenderNode.renderAsync(context, xml, children)

  def renderChangesAsync(context : Context) = 
    RenderNode.renderChangesAsync(context, children)
    
  override def apply(node : RenderNode) : RenderNode = 
    new CompoundRenderNode(node :: children)

  override def & (node : RenderNode) : RenderNode =
    new CompoundRenderNode(children ++ List(node))
}

trait SyncRenderNode extends RenderNode {

  final def renderAsync(context : Context, xml : NodeSeq) : Future[NodeSeq] = 
    Future.successful(render(context, xml)) 
    
  final def renderChangesAsync(context : Context) : Future[JSCmd] = 
    Future.successful(renderChanges(context)) 

  def render(context : Context, xml : NodeSeq) : NodeSeq
  
  def renderChanges(context : Context) : JSCmd
  
  override def apply(node : RenderNode) : RenderNode = node match {
    case node : SyncRenderNode => new CompoundSyncRenderNode(node :: this :: Nil)
    case node => new CompoundRenderNode(node :: this :: Nil)
  }
  
  override def & (node : RenderNode) : RenderNode = node match {
    case node : SyncRenderNode => new CompoundSyncRenderNode(this :: node :: Nil)
    case node => new CompoundRenderNode(this :: node :: Nil)
  }
  
  def apply(node : SyncRenderNode) : RenderNode = 
    new CompoundSyncRenderNode(node :: this :: Nil)
  
  def & (node : SyncRenderNode) : RenderNode = 
    new CompoundSyncRenderNode(this :: node :: Nil)
}

final class CompoundSyncRenderNode(val children: List[SyncRenderNode]) extends SyncRenderNode with MultiChildrenRenderNode {
  def render(context : Context, xml : NodeSeq) =
    children.foldLeft(xml)((xml, node) => node.render(context, xml))

  def renderChanges(context : Context) =
    children.foldLeft[JSCmd](JsNoop)((cmd, node) => node.renderChanges(context))
    
  override def apply(node : SyncRenderNode) : RenderNode = 
    new CompoundSyncRenderNode(node :: children)

  override def & (node : SyncRenderNode) : RenderNode =
    new CompoundSyncRenderNode(children ++ List(node))

  override def apply(node : RenderNode) : RenderNode =
    super.apply(node)

  override def & (node : RenderNode) : RenderNode =
    super.&(node)
}

object IdentRenderNode extends ElemModifier with NoChildRenderNode {
  def render(context : Context, elem : Elem) = elem
  def renderChanges(context: net.scalaleafs2.Context): net.scalaleafs2.JSCmd = JsNoop
  override def & (modifier : ElemModifier) = modifier 
  override def & (node : RenderNode) = node 
  override def apply (modifier : ElemModifier) = modifier
  override def apply (node : RenderNode) = node
  val modify = (context : Context, elem : Elem) => elem  
}

/**
 * Render node delegate. Useful for classes that express a render node
 * using CssTransformations, for example.
 */
trait HasRenderNode extends RenderNode {
  def delegate : RenderNode

  def renderAsync(context : Context, xml : NodeSeq) = 
    delegate.renderAsync(context, xml)

  def renderChangesAsync(context : Context) = 
    delegate.renderChangesAsync(context)
    
  def dispose(context : Context) =
    delegate.dispose(context)
}

/**
 * Render node that expects an Elem as input. 
 * If the elem is not an elem, the identity transformation is used.
 * Implementations should override render(context, elem).
 */
trait ExpectElemRenderNode extends RenderNode {
  def renderAsync(context : Context, xml : NodeSeq) : Future[NodeSeq] = xml match {
    case elem : Elem => renderAsync(context, elem)
    case Seq(elem : Elem) => renderAsync(context, elem)
    case xml => Future.successful(xml)
  }
  def renderAsync(context : Context, elem : Elem) : Future[NodeSeq]
}

trait ExpectElemSyncRenderNode extends SyncRenderNode {
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
  def renderAsync(context : Context, elem : Elem) : Future[NodeSeq] = 
    XmlHelpers.getId(elem).trim match {
      case "" => renderAsync(context, XmlHelpers.setId(elem, generatedId), generatedId)
      case id => renderAsync(context, elem, id)
    }
  def renderAsync(context : Context, elem : Elem, id : String) : Future[NodeSeq]
}

trait ExpectElemWithIdSyncRenderNode extends ExpectElemSyncRenderNode {
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
trait ElemModifier extends ExpectElemSyncRenderNode {
  val modify : (Context, Elem) => Elem
  def render(context : Context, elem : Elem) : Elem 
  
  def &(modifier : ElemModifier) : ElemModifier = 
    new CompoundElemModifier(this :: modifier :: Nil)
  
  def apply(modifier : ElemModifier) : ElemModifier = 
    new CompoundElemModifier(modifier :: this :: Nil)
}

final class ConditionalElemModifier(val modify : (Context, Elem) => Elem, condition : => Boolean) extends ElemModifier with NoChildRenderNode {
  
  def render(context : Context, elem : Elem) : Elem = 
    if (condition) modify(context, elem) 
    else elem
    
  def renderChanges(context : Context) = JsNoop
}

final class CompoundElemModifier(val children : List[ElemModifier]) extends ElemModifier with MultiChildrenRenderNode {
  val modify = (context : Context, elem : Elem) => elem

  def render(context : Context, elem : Elem) : Elem =
    children.foldLeft(elem)((elem, child) => child.modify(context, elem))

  def renderChanges(context : Context) =
    children.foldLeft[JSCmd](JsNoop)((cmd, node) => node.renderChanges(context))
    
  override def &(modifier : ElemModifier) : ElemModifier = 
    new CompoundElemModifier(children ++ List(modifier))
  
  override def apply(modifier : ElemModifier) : ElemModifier = 
    new CompoundElemModifier(modifier :: children)
}

