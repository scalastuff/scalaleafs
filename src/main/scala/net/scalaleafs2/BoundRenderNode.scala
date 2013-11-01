package net.scalaleafs2

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.xml.Elem
import scala.xml.NodeSeq

class BoundRenderNode[A](bindable : Bindable, get : Context => Future[A], f : Placeholder[A] => RenderNode) extends ExpectElemWithIdRenderNode with SingleChildRenderNode {
  
  val placeholder = new Placeholder[A]
  val child = f(placeholder)
  var lastElem : Elem = null
  var lastId : String = ""
  var version : Int = -1
    println("CREATED " + this)
  
  def renderAsync(context : Context, elem : Elem, id : String) : Future[NodeSeq] = {
    import context.executionContext
    println("RENDER FIRST TIME: " + version + " new " + bindable.version + " " + this)
    lastElem = elem
    lastId = id
    version = bindable.version
    get(context).flatMap { value =>
      placeholder.value = value
      child.renderAsync(context, elem)
    }
  }
  
  def renderChangesAsync(context : Context) : Future[JSCmd] = {
    import context.executionContext
    if (version != bindable.version) {
      println("Changed: " + version + " new " + bindable.version + " " + this)
      renderAsync(context, lastElem, lastId).map { xml =>
        ReplaceHtml(lastId, xml)
      }
    }
    else
      child.renderChangesAsync(context)
  }
}

class BoundSyncRenderNode[A](bindable : Bindable, get : Context => A, f : Placeholder[A] => RenderNode) extends ExpectElemWithIdRenderNode with SingleChildRenderNode {
  
  val placeholder = new Placeholder[A]
  val child = f(placeholder)
  var lastElem : Elem = null
  var lastId : String = ""
  var version : Int = -1
  
  def render(context : Context, elem : Elem, id : String) : NodeSeq = {
    import context.executionContext
    lastElem = elem
    lastId = id
    version = bindable.version
    println("Setting value " + get(context) + " to placeholder " + placeholder)
    placeholder.value = get(context)
    child.render(context, elem)
  }
  
  def renderChanges(context : Context) : JSCmd = {
    import context.executionContext
    if (version != bindable.version)
      ReplaceHtml(lastId, render(context, lastElem))
    else
      child.renderChanges(context)
  }
}

class BoundAllRenderNode[A, F](bindable : Bindable, f : Placeholder[A] => RenderNode) extends ExpectElemWithIdRenderNode with MultiChildrenRenderNode {
  
  val placeholders = ArrayBuffer[Placeholder[A]]()
  val children = ArrayBuffer[RenderNode]()
  var lastElem : Elem = null
  var lastId : String = ""
  var version : Int = -1
  
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
      import context.executionContext
      if (version != bindable.version) {
        renderChanges0(context, lastElem, lastId, { xml =>
          RemoveNextSiblings(lastId, lastId) & ReplaceHtml(lastId, xml)
        })
      }
      else
        children.foldLeft(JsNoop.asInstanceOf[JSCmd])(_ & _.renderChanges(context))
  }
}

class AsyncBoundAllRenderNode[A](asyncVal : AsyncVal[_ <: Iterable[A]], f : Placeholder[A] => RenderNode) extends BoundAllRenderNode[A, Future[_ <: Iterable[A]]](asyncVal, f) {
  
  def renderValues0(context : Context, elem : Elem, id : String) : NodeSeq = 
    context.postponeRender(elem, asyncVal.get(context), renderValues(context, elem, id))

  def renderChanges0(context : Context, elem : Elem, id : String, f : NodeSeq => JSCmd) : JSCmd = 
    context.postponeRenderChanges(lastId, asyncVal.get(context), renderValues(context, lastElem, lastId), f)
}

class SyncBoundAllRenderNode[A](syncVal : SyncVal[_ <: Iterable[A]], f : Placeholder[A] => RenderNode) extends BoundAllRenderNode[A, Future[_ <: Iterable[A]]](syncVal, f) {
  
  def renderValues0(context : Context, elem : Elem, id : String) : NodeSeq = 
    renderValues(context, elem, id)(syncVal.get(context))

  def renderChanges0(context : Context, elem : Elem, id : String, f : NodeSeq => JSCmd) : JSCmd = 
    f(renderValues(context, elem, id)(syncVal.get(context)))
}



