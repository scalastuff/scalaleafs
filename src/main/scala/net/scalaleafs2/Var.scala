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

/**
 * A Var is a wrapper for a mutable value. One can listen for changes, or map a Var to other Vars that become
 * dependent on it. Vars can also be bound to a XML, transforming XML based on the current value of the Var. When
 * the Var changes (or one of the Vars it depends on), the transformation is performed again. The piece of 
 * XML that was created by the transformation is sent to the browser using a ReplaceHtml JavaScript command.
 */
trait Var[A] { thisVar =>
  private[this] val listeners = new DisposableList[VarListener[A]]
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
  def onChange(f : A => Unit) : VarListener[A] = 
    listeners.add(new VarListener[A](f))
  
  /**
   * Maps this var to another one.
   */
  def map[B](f : A => B) : Var[B] = {
    val targetVar = new Var[B] {  
      var value = f(thisVar.get)
    }
    listeners.add(new VarListener[A](a => targetVar.set(f(a))) {
      val targetRef = new WeakReference[Any](targetVar)
      override def shouldBeDisposed = targetRef.get == null
    })
    targetVar
  }
  
  def bind(f : => Placeholder[A] => RenderNode) = new ExpectElemWithIdRenderNode {
    var lastElem : Elem = null
    var lastId : String = null
    var child : RenderNode = null
    var dirty : Boolean = false
    var placeholder : Placeholder[A] = null
    val listener = onChange(_ => dirty = true)
    def render(context : Context, elem : Elem, id : String) : NodeSeq = {
      lastElem = elem
      lastId = id
      if (child == null) {
        placeholder = new Placeholder[A](thisVar)
        child = f(placeholder)
      }
      child.render(context, elem)
    }
    override def dispose(context : Context) = {
      super.dispose(context)
      listener.dispose
    }
    
    def renderChanges(context : Context) {
      if (dirty && child != null) context.addEagerPostRequestJs(ReplaceHtml(lastId, child.render(context, lastElem)))
      else super.renderChanges(context)
    }

    def children = child match { 
      case null => Seq.empty
      case child => Seq(child)
    }
  }
}

class IterableVar[A <: Iterable[A]] extends Var[A] { thisVar =>
  
  def bindAll(f : => Placeholder[A] => RenderNode) = new ExpectElemWithIdRenderNode {
    var lastElem : Elem = null
    var lastId : String = null
    var child : RenderNode = null
    var dirty : Boolean = false
    var placeholder : Placeholder[A] = null
    val listener = onChange(_ => dirty = true)
    def render(context : Context, elem : Elem, id : String) : NodeSeq = {
      lastElem = elem
      lastId = id
      if (child == null) {
        placeholder = new Placeholder[A](thisVar)
        child = f(placeholder)
      }
      child.render(context, elem)
    }
    override def dispose(context : Context) = {
      super.dispose(context)
      listener.dispose
    }
    
    def renderChanges(context : Context) {
      if (dirty && child != null) context.addEagerPostRequestJs(ReplaceHtml(lastId, child.render(context, lastElem)))
      else super.renderChanges(context)
    }

    def children = child match { 
      case null => Seq.empty
      case child => Seq(child)
    }
  }
}

object Var {

  def apply[A](initialValue : A) = new Var[A] {
    var value = initialValue
  } 
}

class VarListener[A](val notifyChanged : A => Unit) extends Disposable 
