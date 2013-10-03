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

/**
 * A Var is a wrapper for a mutable value. One can listen for changes, or map a Var to other Vars that become
 * dependent on it. Vars can also be bound to a XML, transforming XML based on the current value of the Var. When
 * the Var changes (or one of the Vars it depends on), the transformation is performed again. The piece of 
 * XML that was created by the transformation is sent to the browser using a ReplaceHtml JavaScript command.
 */
trait Var[A] extends Disposable { thisVar =>
  class VarListener(val notifyChanged : A => Unit) extends Disposable 
  private[this] val listeners = new DisposableList[VarListener]
  protected[this] var value : A 
  
  /**
   * Get the current value of the var.
   */
  def get = value

  /**
   * Sets the current value of the var. Listeners and dependent vars are notified
   * when the value is changed.
   */
  def set(value : A) {
    if (this.value != value) {
      this.value = value
      listeners.foreach(_.notifyChanged(value))
    }
  }
    
  /**
   * Modify the current value. It's a combined get and set. Listeners and dependent vars are notified
   * when the value is changed.
   */
  def modify(f : A => A) {
    set(f(value))
  }
    
  /**
   * Registers a change listener for this var.
   */
  private[scalaleafs2] def onChange(f : A => Unit) : Disposable = 
    listeners.add(new VarListener(f))
  
  private[scalaleafs2] def dispose = 
    listeners.clear
    
  /**
   * Maps this var to another one.
   * Support both explicit and implicit disposals.
   */
  def map[B](f : A => B) : Var[B] = {
    val targetVar : Var[B] = new Var[B] {  
      var value = f(thisVar.get)
      override def dispose = {
        super.dispose
        listener.dispose
      }
    }
    val listener = listeners.add(new VarListener(a => targetVar.set(f(a))) {
      val targetRef = new WeakReference[Any](targetVar)
      override def shouldBeDisposed = targetRef.get == null
    })
    targetVar
  }
}

class BoundVar[A, B](theVar : Var[A], getValues : => Iterable[B], f : => Placeholder[B] => RenderNode) extends ExpectElemWithIdRenderNode {
  var lastElem : Elem = null
  var lastId : String = null
  var child : RenderNode = null
  var dirty : Boolean = false
  var maxSize : Int = 0
  var placeholder : Placeholder[A] = null
  val children = ArrayBuffer[RenderNode]()
  val vars = ArrayBuffer[Var[B]]()
  val listener = theVar.onChange(_ => dirty = true)
  def render(context : Context, elem : Elem, id : String) : NodeSeq = {
    lastElem = elem
    lastId = id
    dirty = false
    val values = getValues.toSeq
    while (children.size < values.size) 
      children += f(new Placeholder[B](values, children.size, mkVar)) // change
    children.dropRight(children.size - values.size)
    if (maxSize < children.size)
      maxSize = children.size
    children.zipWithIndex.flatMap {
      case (child, 0) => child.render(context, elem) 
      case (child, i) => child.render(context, XmlHelpers.setId(elem, id + "-" + i)) 
    } 
  }
  
  override def dispose(context : Context) = {
    super.dispose(context)
    listener.dispose
    vars.foreach(_.dispose)
  }
  
  def renderChanges(context : Context) {
    if (dirty && lastElem != null) 
      context.addEagerPostRequestJs(
        if (maxSize > 1) RemoveNextSiblings(lastId, lastId) & ReplaceHtml(lastId, render(context, lastElem)) 
        else ReplaceHtml(lastId, render(context, lastElem)))
    else 
      super.renderChanges(context)
  }
  
  def mkVar = { (index : Int) =>
    val newVar = theVar.map(_ => getValues.toSeq(index)) 
    vars += newVar
    newVar
  }
}

object Var {

  def apply[A](initialValue : A) = new Var[A] {
    var value = initialValue
  }
  
  implicit class RichVar[A](val theVar : Var[A]) extends AnyVal {
    def bind(f : => Placeholder[A] => RenderNode) = 
      new BoundVar[A, A](theVar, Iterable(theVar.get), f)
  }
  
  implicit class RichOptionVar[A](val theVar : Var[_ <: Option[A]]) extends AnyVal {
    def bindAll(f : => Placeholder[A] => RenderNode) = 
      new BoundVar(theVar, theVar.get, f)
  }
  
  implicit class RichIterableVar[A](val theVar : Var[_ <: Iterable[A]]) extends AnyVal {
    def bindAll(f : => Placeholder[A] => RenderNode) = 
      new BoundVar(theVar, theVar.get, f)
  }
}


