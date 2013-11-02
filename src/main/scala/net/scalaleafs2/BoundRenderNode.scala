package net.scalaleafs2

import scala.collection.mutable.ArrayBuffer
import scala.xml.Elem
import scala.xml.NodeSeq

/**
 * Rendernode that handles a binding.
 */
abstract class BoundRenderNode[A](bindable : Bindable, f : Placeholder[A] => RenderNode) extends ExpectElemWithIdRenderNode with SingleChildRenderNode {
  
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
    renderValue0(context, elem, id)
  }
  
  def renderValue0(context : Context, elem : Elem, id : String) : NodeSeq
  def renderChanges0(context : Context, elem : Elem, id : String, f : NodeSeq => JSCmd) : JSCmd

  protected def renderValue(context : Context, elem : Elem, id : String)(value : A) : NodeSeq = {
    placeholder.value = value
    child.render(context, elem)
  }
  
  def renderChanges(context : Context) : JSCmd = {
      if (version != bindable.version) {
        renderChanges0(context, lastElem, lastId, { xml =>
          ReplaceHtml(lastId, xml)
        })
      }
      else
        child.renderChanges(context)
  }
}

class AsyncBoundRenderNode[A](asyncVal : AsyncVal[A], f : Placeholder[A] => RenderNode) extends BoundRenderNode[A](asyncVal, f) {
  
  def renderValue0(context : Context, elem : Elem, id : String) : NodeSeq = 
    context.renderAsync(elem, asyncVal.get(context).map(renderValue(context, elem, id))(context.executionContext))

  def renderChanges0(context : Context, elem : Elem, id : String, f : NodeSeq => JSCmd) : JSCmd = 
    context.renderChangesAsync(asyncVal.get(context).map(renderValue(context, elem, id))(context.executionContext), f)
}

class SyncBoundRenderNode[A](syncVal : SyncVal[A], f : Placeholder[A] => RenderNode) extends BoundRenderNode[A](syncVal, f) {
  
  def renderValue0(context : Context, elem : Elem, id : String) : NodeSeq = 
    renderValue(context, elem, id)(syncVal.get(context))

  def renderChanges0(context : Context, elem : Elem, id : String, f : NodeSeq => JSCmd) : JSCmd = 
    f(renderValue(context, elem, id)(syncVal.get(context)))
}

abstract class BoundAllRenderNode[A](bindable : Bindable, f : Placeholder[A] => RenderNode) extends ExpectElemWithIdRenderNode with MultiChildrenRenderNode {
  
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
    renderValues0(context, elem, id)
  }
  
  def renderValues0(context : Context, elem : Elem, id : String) : NodeSeq
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
    children.zip(values).zipWithIndex.flatMap {
      case ((child, value), index) =>
        placeholders(index).value = value
        child.render(context, 
          if (index == 0) elem 
          else XmlHelpers.setId(elem, id + "-" + index)) 
    } 
  }
  
  def renderChanges(context : Context) : JSCmd = {
      if (version != bindable.version) {
        renderChanges0(context, lastElem, lastId, { xml =>
          RemoveNextSiblings(lastId, lastId) & ReplaceHtml(lastId, xml)
        })
      }
      else
        children.foldLeft(JSNoop.asInstanceOf[JSCmd])(_ & _.renderChanges(context))
  }
}

class AsyncBoundAllRenderNode[A](asyncVal : AsyncVal[_ <: Iterable[A]], f : Placeholder[A] => RenderNode) extends BoundAllRenderNode[A](asyncVal, f) {
  
  def renderValues0(context : Context, elem : Elem, id : String) : NodeSeq = 
    context.renderAsync(elem, asyncVal.get(context).map(renderValues(context, elem, id))(context.executionContext))

  def renderChanges0(context : Context, elem : Elem, id : String, f : NodeSeq => JSCmd) : JSCmd = 
    context.renderChangesAsync(asyncVal.get(context).map(renderValues(context, elem, id))(context.executionContext), f)
}

class SyncBoundAllRenderNode[A](syncVal : SyncVal[_ <: Iterable[A]], f : Placeholder[A] => RenderNode) extends BoundAllRenderNode[A](syncVal, f) {
  
  def renderValues0(context : Context, elem : Elem, id : String) : NodeSeq = 
    renderValues(context, elem, id)(syncVal.get(context))

  def renderChanges0(context : Context, elem : Elem, id : String, f : NodeSeq => JSCmd) : JSCmd = 
    f(renderValues(context, elem, id)(syncVal.get(context)))
}
