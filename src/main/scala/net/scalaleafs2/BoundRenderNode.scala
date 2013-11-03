package net.scalaleafs2

import scala.collection.mutable.ArrayBuffer
import scala.xml.Elem
import scala.xml.NodeSeq

/**
 * Rendernode that handles a binding.
 */
class BoundRenderNode[A](bindable : Val[A], f : Placeholder[A] => RenderNode) extends ExpectElemWithIdRenderNode with SingleChildRenderNode {
  
  private val placeholder = new Placeholder[A]
  val child = f(placeholder)
  private var lastElem : Elem = null
  private var lastId : String = ""
  private var version : Int = -1
  
  def render(context : Context, elem : Elem, id : String) : NodeSeq = {
    import context.executionContext
    lastElem = elem
    lastId = id
    version = bindable.version(context)
    if (bindable.isAsync)
      context.renderAsync(elem, bindable.getAsync(context).map(renderValue(context, elem, id))(context.executionContext))
    else
      renderValue(context, elem, id)(bindable.get(context))
  }
  
  def renderChanges0(context : Context, elem : Elem, id : String, f : NodeSeq => JSCmd) : JSCmd = 
    if (bindable.isAsync)
      context.renderChangesAsync(bindable.getAsync(context).map(renderValue(context, elem, id))(context.executionContext), f)    
    else
      f(renderValue(context, elem, id)(bindable.get(context)))


  protected def renderValue(context : Context, elem : Elem, id : String)(value : A) : NodeSeq = {
    placeholder.value = value
    child.render(context, elem)
  }
  
  def renderChanges(context : Context) : JSCmd = {
      if (version != bindable.version(context)) {
        version = bindable.version(context)
        renderChanges0(context, lastElem, lastId, { xml =>
          ReplaceHtml(lastId, xml)
        })
      }
      else
        child.renderChanges(context)
  }
}

class BoundAllRenderNode[A](bindable : Val[_ <: Iterable[A]], f : Placeholder[A] => RenderNode) extends ExpectElemWithIdRenderNode with MultiChildrenRenderNode {
  
  private val placeholders = ArrayBuffer[Placeholder[A]]()
  val children = ArrayBuffer[RenderNode]()
  private var lastElem : Elem = null
  private var lastId : String = ""
  private var version : Int = -1
  
  def render(context : Context, elem : Elem, id : String) : NodeSeq = {
    import context.executionContext
    lastElem = elem
    lastId = id
    version = bindable.version(context)
    if (bindable.isAsync) 
      context.renderAsync(elem, bindable.getAsync(context).map(renderValues(context, elem, id))(context.executionContext))
    else 
      renderValues(context, elem, id)(bindable.get(context))
  }
  
  def renderChanges0(context : Context, elem : Elem, id : String, f : NodeSeq => JSCmd) : JSCmd = 
    if (bindable.isAsync) 
      context.renderChangesAsync(bindable.getAsync(context).map(renderValues(context, elem, id))(context.executionContext), f)
    else 
      f(renderValues(context, elem, id)(bindable.get(context)))


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
      if (version != bindable.version(context)) {
        version = bindable.version(context)
        renderChanges0(context, lastElem, lastId, { xml =>
          if (version != bindable.version(context)) 
            RemoveNextSiblings(lastId, lastId) & ReplaceHtml(lastId, xml)
          else Noop
        })
      } else
        children.foldLeft(Noop.asInstanceOf[JSCmd])(_ & _.renderChanges(context))
  }
}

