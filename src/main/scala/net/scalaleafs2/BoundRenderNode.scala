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

class BoundSyncRenderNode[A](bindable : Bindable, get : Context => A, f : Placeholder[A] => SyncRenderNode) extends ExpectElemWithIdSyncRenderNode with SingleChildRenderNode {
  
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

class BoundAllRenderNode[A](bindable : AsyncVal[_ <: Iterable[A]], f : Placeholder[A] => RenderNode) extends ExpectElemWithIdRenderNode with MultiChildrenRenderNode {
  
  val placeholders = ArrayBuffer[Placeholder[A]]()
  val children = ArrayBuffer[RenderNode]()
  var lastElem : Elem = null
  var lastId : String = ""
  var version : Int = -1
  
  def renderAsync(context : Context, elem : Elem, id : String) : Future[NodeSeq] = {
    import context.executionContext
    lastElem = elem
    lastId = id
    version = bindable.version
    bindable.getAsync(context).flatMap {
      values =>
      for (i <- values.size.until(children.size)) children(i).dispose(context)
      placeholders.remove(placeholders.size, placeholders.size - values.size)
      children.remove(children.size, children.size - values.size)
      while (children.size < values.size)  {
        val placeholder = new Placeholder[A] 
        val child = f(placeholder)
        placeholders += placeholder
        children += child
      }
      Future.sequence (
        children.zip(values).zipWithIndex.map {
          case ((child, value), index) =>
            placeholders(index).value = value
            child.renderAsync(context, 
              if (index == 0) elem 
              else XmlHelpers.setId(elem, id + "-" + index)) 
        } 
      ).map(_.flatten)
    }
  }
  
  def renderChangesAsync(context : Context) : Future[JSCmd] = {
      import context.executionContext
      if (version != bindable.version)
        renderAsync(context, lastElem).map { xml =>
          RemoveNextSiblings(lastId, lastId) & ReplaceHtml(lastId, xml)
        }
      else
        Future.fold(children.map(_.renderChangesAsync(context)))(JsNoop.asInstanceOf[JSCmd])(_ & _)
  }
}

class BoundAllSyncRenderNode[A](bindable : SyncVal[_ <: Iterable[A]], f : Placeholder[A] => SyncRenderNode) extends ExpectElemWithIdSyncRenderNode with MultiChildrenRenderNode {
  
  val placeholders = ArrayBuffer[Placeholder[A]]()
  val children = ArrayBuffer[SyncRenderNode]()
  var lastElem : Elem = null
  var lastId : String = ""
  var version : Int = -1

  def render(context : Context, elem : Elem, id : String) : NodeSeq = {
    import context.executionContext
    lastElem = elem
    lastId = id
    version = bindable.version
    val values = bindable.get(context)
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
      if (version != bindable.version)
        RemoveNextSiblings(lastId, lastId) & ReplaceHtml(lastId, render(context, lastElem))
      else
        children.foldLeft[JSCmd](JsNoop)(_ & _.renderChanges(context))
  }
}


