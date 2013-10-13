/**
 * Copyright (c) 2012 Ruud Diterwich.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package net.scalaleafs2

import scala.xml.NodeSeq
import scala.xml.Elem
import java.util.UUID
import java.lang.ref.WeakReference
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

/**
 * A Bindable is a wrapper for a value. One can listen for changes, or map a Bindable to other Bindables that become
 * dependent on it. Bindables can also be bound to a XML, transforming XML based on the current value of the Bindable. When
 * the is triggered (or one of the Bindables it depends on), the transformation is performed again. The piece of 
 * XML that was created by the transformation is sent to the browser using a ReplaceHtml JavaScript command.
 */
trait Bindable[A] { thisBindable =>
  private var _version : Int = 0
  
  /**
   * Get the current value of the var.
   */
  def get(context : Context) : A

  /**
   * Triggers this value. All listeners will be triggered as well.
   */
  def trigger : Unit = 
    _version += 1
  
  def version : Int = 
    _version
    
  /**
   * Registers a change listener for this var.
   */
  private[scalaleafs2] def onChange(f : A => Unit) : Disposable = 
    listeners.add(new BindableListener(f))
    
  /**
   * Maps this val to another one.
   * Support both explicit and implicit disposals.
   */
  def map[B](f : A => B) : Bindable[B] = {
    val targetBindable : Bindable[B] = new Bindable[B] with BindableList {  
      def get(context : Context) : Future[B] = 
        thisBindable.get(context).map(f)(context.executionContext)
    }
    val listener = listeners.add(new BindableListener {
      val targetRef = new WeakReference[Bindable[B]](targetBindable)
      def changed(targetBindable : A) = targetRef.get match {
        case null => disposeFromList
        case targetBindable => targetBindable.trigger 
      }
    })
    targetBindable
  }
}

trait AsyncBindable[A] extends Bindable[A] {
  def get(context : Context) : Future[A]
}

trait SyncBindable[A] extends Bindable[A] {
  def get(context : Context) : A
}

class Def[A](f : Context => A) extends Bindable[A] {
}


class AsyncBoundRenderNode[A](bindable : AsyncBindable[A], f : Placeholder[A] => RenderNode) extends ExpectElemWithIdAsyncRenderNode {
  
  val placeholder = new Placeholder[A](null.asInstanceOf[A])
  val child = f(placeholder)
  var lastElem : Elem = null
  var lastId : String = ""
  var version : Int = -1
  
  def renderAsync(context : Context, elem : Elem, id : String) : Future[NodeSeq] = {
    lastElem = elem
    lastId = id
    version = bindable.version
    child.renderAsync(context, lastElem)
  }
  
  def renderChangesAsync(context : Context) : Future[JSCmd] = {
    import context.executionContext
    if (version != bindable.version)
      bindable.get(context).flatMap { value =>
        placeholder.value = value
        renderAsync(context, lastElem).map { xml =>
          ReplaceHtml(lastId, xml)
        }
    } else {
      child.renderChangesAsync(context)
    }
  }
}

class AsyncBoundAllRenderNode[B, A <: Iterable[B]](bindable : AsyncBindable[A], f : Placeholder[B] => RenderNode) extends ExpectElemWithIdAsyncRenderNode {
  
  val placeholders = ArrayBuffer[Placeholder[A]]()
  val children = ArrayBuffer[RenderNode]()
  var lastElem : Elem = null
  var lastId : String = ""
  var version : Int = -1
  
  def renderAsync(context : Context, elem : Elem, id : String) : Future[NodeSeq] = {
    lastElem = elem
    lastId = id
    version = bindable.version
    placeholders.dropRight(placeholders.size - values.size)
    children.dropRight(children.size - values.size)
    while (children.size < values.size)  {
      val index = children.size
      val placeholder = new Placeholder[B](values(index)) 
      placeholders += placeholder
      children += f(placeholder)
    }
    if (maxSize < children.size)
      maxSize = children.size
    children.zipWithIndex.flatMap {
      case (child, index) =>
        placeholders(index).value = values(index)
        child.render(context, 
          if (index == 0) elem 
          else XmlHelpers.setId(elem, id + "-" + index)) 
    } 
    child.renderAsync(context, lastElem)
  }
  
  def renderChangesAsync(context : Context) : Future[JSCmd] = {
      import context.executionContext
      if (version != bindable.version)
        bindable.get(context).flatMap { value =>
        placeholder.value = value
        renderAsync(context, lastElem).map { xml =>
        ReplaceHtml(lastId, xml)
        }
      } else {
        child.renderChangesAsync(context)
      }
  }
}
