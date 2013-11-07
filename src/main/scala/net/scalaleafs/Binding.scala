package net.scalaleafs

import scala.collection.mutable.ArrayBuffer
import scala.xml.Elem
import scala.xml.NodeSeq
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

object Binding extends Binding

trait Binding {
  def bind[A](bindable : Bindable[A])(f : Placeholder[A] => RenderNode) =
    bindable match {
      case v : Val[A] => new BoundRenderNode(v, f)
      case v : AsyncVal[A] => new AsyncBoundRenderNode(v, f)
    }

  def bindOption[A](bindable : Bindable[Option[A]])(f : Placeholder[A] => RenderNode) = 
    bindable match {
      case v : Val[Option[A]] => new BoundAllRenderNode(new Val[Iterable[A]] {
        override def version = v.version
        def get = v.get
      }, f)
      case v : AsyncVal[Option[A]] => new AsyncBoundAllRenderNode(new AsyncVal[Iterable[A]] {
        implicit val ec : ExecutionContext = v.ec
        def get : Future[Iterable[A]] = v.get.map(_.toIterable)
      }, f)
    }
  
  def bindAll[A, B <: Iterable[A]](bindable : Bindable[B])(f : Placeholder[A] => RenderNode) = 
    bindable match {
      case v : Val[B] => new BoundAllRenderNode(v, f)
      case v : AsyncVal[B] => new AsyncBoundAllRenderNode(v, f)
    }
}
  
/**
 * Rendernode that handles a binding.
 */
abstract class AbstractBoundRenderNode[A](bindable : Bindable[_], f : Placeholder[A] => RenderNode) extends ExpectElemWithIdRenderNode with SingleChildRenderNode {
  
  private val placeholder = new Placeholder[A]
  val child = f(placeholder)
  private var lastElem : Elem = null
  private var lastId : String = ""
  private var version : Int = -1
  
  def render(context : Context, elem : Elem, id : String) : NodeSeq = {
    import context.executionContext
    lastElem = elem
    lastId = id
    version = bindable.version
    render0(context, elem, id)
  }
  
  def render0(context : Context, elem : Elem, id : String) : NodeSeq
  def renderChanges0(context : Context, elem : Elem, id : String, f : NodeSeq => JSCmd) : JSCmd 

  protected def renderValue(context : Context, elem : Elem, id : String)(value : A) : NodeSeq = {
    placeholder.value = value
    postProcess(child.render(context, elem))
  }
  
  override def renderChanges(context : Context) : JSCmd = {
      if (version != bindable.version) {
        version = bindable.version
        renderChanges0(context, lastElem, lastId, { xml =>
          ReplaceHtml(lastId, xml)
        })
      }
      else
        child.renderChanges(context)
  }
}

class BoundRenderNode[A](bindable : Val[A], f : Placeholder[A] => RenderNode) extends AbstractBoundRenderNode(bindable, f) {
  def render0(context : Context, elem : Elem, id : String) = 
    renderValue(context, elem, id)(bindable.get)
  def renderChanges0(context : Context, elem : Elem, id : String, f : NodeSeq => JSCmd) : JSCmd = 
    f(renderValue(context, elem, id)(bindable.get))
}

class AsyncBoundRenderNode[A](bindable : AsyncVal[A], f : Placeholder[A] => RenderNode) extends AbstractBoundRenderNode(bindable, f) {
  def render0(context : Context, elem : Elem, id : String) = 
    context.renderAsync(elem, bindable.get.map(renderValue(context, elem, id))(context.executionContext))
  def renderChanges0(context : Context, elem : Elem, id : String, f : NodeSeq => JSCmd) : JSCmd = 
    context.renderChangesAsync(bindable.get.map(renderValue(context, elem, id))(context.executionContext), f)    
}

abstract class AbstractBoundAllRenderNode[A](bindable : Bindable[_], f : Placeholder[A] => RenderNode) extends ExpectElemWithIdRenderNode with MultiChildrenRenderNode {
  
  private val placeholders = ArrayBuffer[Placeholder[A]]()
  val children = ArrayBuffer[RenderNode]()
  private var lastElem : Elem = null
  private var lastId : String = ""
  private var version : Int = -1
  
  def render(context : Context, elem : Elem, id : String) : NodeSeq = {
    import context.executionContext
    lastElem = elem
    lastId = id
    version = bindable.version
    render0(context, elem, id)
  }
  
  def render0(context : Context, elem : Elem, id : String) : NodeSeq 
  def renderChanges0(context : Context, elem : Elem, id : String, f : NodeSeq => JSCmd) : JSCmd  

  protected def renderValues(context : Context, elem : Elem, id : String)(values : Iterable[A]) : NodeSeq = {
    for (i <- values.size.until(children.size)) children(i).dispose(context)
    placeholders.remove(placeholders.size, placeholders.size - values.size)
    children.remove(children.size, children.size - values.size)
    while (children.size < values.size)  {
      val placeholder = new Placeholder[A]  
      val child = f(placeholder)
      placeholders += placeholder
      children += child
    }
    postProcess(children.zip(values).zipWithIndex.flatMap {
      case ((child, value), index) =>
        placeholders(index).value = value
        child.render(context, 
          if (index == 0) elem 
          else XmlHelpers.setId(elem, id + "-" + index)) 
    })
  }
  
  override def renderChanges(context : Context) : JSCmd = {
      if (version != bindable.version) {
        version = bindable.version
        renderChanges0(context, lastElem, lastId, { xml =>
          if (version != bindable.version) 
            RemoveNextSiblings(lastId, lastId) & ReplaceHtml(lastId, xml)
          else Noop
        })
      } else
        children.foldLeft(Noop.asInstanceOf[JSCmd])(_ & _.renderChanges(context))
  }
}

class BoundAllRenderNode[A](bindable : Val[_ <: Iterable[A]], f : Placeholder[A] => RenderNode) extends AbstractBoundAllRenderNode[A](bindable, f) {
  
  def render0(context : Context, elem : Elem, id : String) : NodeSeq = 
    renderValues(context, elem, id)(bindable.get)
  def renderChanges0(context : Context, elem : Elem, id : String, f : NodeSeq => JSCmd) : JSCmd = 
    f(renderValues(context, elem, id)(bindable.get))
}

class AsyncBoundAllRenderNode[A](bindable : AsyncVal[_ <: Iterable[A]], f : Placeholder[A] => RenderNode) extends AbstractBoundAllRenderNode[A](bindable, f) {
  def render0(context : Context, elem : Elem, id : String) : NodeSeq = 
    context.renderAsync(elem, bindable.get.map(renderValues(context, elem, id))(context.executionContext))
  def renderChanges0(context : Context, elem : Elem, id : String, f : NodeSeq => JSCmd) : JSCmd = 
    context.renderChangesAsync(bindable.get.map(renderValues(context, elem, id))(context.executionContext), f)
}