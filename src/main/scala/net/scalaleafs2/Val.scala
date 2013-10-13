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
 * A Val is a wrapper for a value. One can listen for changes, or map a Val to other Vals that become
 * dependent on it. Vals can also be bound to a XML, transforming XML based on the current value of the Val. When
 * the is triggered (or one of the Vals it depends on), the transformation is performed again. The piece of 
 * XML that was created by the transformation is sent to the browser using a ReplaceHtml JavaScript command.
 */
trait Val[A] { thisVal =>
  trait ValListener extends Disposable {
    def changed(a : A) : Unit
  }
  private[this] val listeners = new DisposableList[ValListener]
  
  /**
   * Get the current value of the var.
   */
  def get : A

  /**
   * Triggers this value. The getter will be re-evaluated, and all listeners will be notified.
   */
  def trigger : Unit = 
    notifyListeners(get)
  
  protected def notifyListeners(value : A) =
    listeners.foreach(_.changed(value))
  
  /**
   * Registers a change listener for this var.
   */
  private[scalaleafs2] def onChange(f : A => Unit) : Disposable = 
    listeners.add(new ValListener(f))
    
  /**
   * Maps this val to another one.
   * Support both explicit and implicit disposals.
   */
  def map[B](f : A => B) : Val[B] = {
    val targetVal : Val[B] = new Val[B] {  
      def get = f(thisVal.get)
    }
    val listener = listeners.add(new ValListener {
      val targetRef = new WeakReference[Val[B]](targetVal)
      def changed(targetVal : A) = targetRef.get match {
        case null => disposeFromList
        case targetVal => targetVal.trigger 
      }
    })
    targetVal
  }
}

object ValPlaceholder {
  implicit def toA[A](e : ValPlaceholder[A]) : A = e.get
  implicit def toVal[A](p : ValPlaceholder[A]) : Val[A] = p.toVal
}

class ValPlaceholder[A](initialValue : A, mkVal : => Val[A]) {
  private[scalaleafs2] var value : A = initialValue
  def get : A = value
  lazy val toVal : Val[A] = mkVal
}

class BoundVal[A, B](theVal : Val[A], getValues : => Iterable[B], f : => ValPlaceholder[B] => RenderNode) extends ExpectElemWithIdRenderNode {
  var lastElem : Elem = null
  var lastId : String = null
  var dirtyValue : Option[A] = None
  var maxSize : Int = 0
  val placeholders = ArrayBuffer[ValPlaceholder[B]]()
  val children = ArrayBuffer[RenderNode]()
  val vars = ArrayBuffer[Val[B]]()
  val listener = theVal.onChange(value => dirtyValue = Some(value))
  def render(context : Context, elem : Elem, id : String) : NodeSeq = {
    val values = getValues.toSeq
    placeholders.dropRight(placeholders.size - values.size)
    children.dropRight(children.size - values.size)
    while (children.size < values.size)  {
      val index = children.size
      val placeholder = new ValPlaceholder[B](values(index), mkVal(index)) 
      placeholders += placeholder
      children += f(placeholder)
    }
    val result = children.zipWithIndex.flatMap {
      case (child, index) =>
        placeholders(index).value = values(index)
        child.render(context, 
          if (index == 0) elem 
          else XmlHelpers.setId(elem, id + "-" + index)) 
    } 
    lastElem = elem
    lastId = id
    if (maxSize < children.size)
      maxSize = children.size
    dirtyValue = None
    result
  }
  
  override def dispose(context : Context) = {
    super.dispose(context)
    dirtyValue = None
    listener.dispose
    vars.foreach(_.dispose)
  }
  
  override def renderChanges(context : Context) : JSCmd = {
    val cmd = dirtyValue match {
      case Some(value) if last =>
        if (maxSize > 1) RemoveNextSiblings(lastId, lastId) & ReplaceHtml(lastId, render(context, lastElem)) 
        else ReplaceHtml(lastId, render(context, lastElem))
      case none =>
        super.renderChanges(context)
    }
    dirtyValue = None
    cmd
  }
  
  def mkVal(index : Int) = {
    val newVal = theVal.map(_ => getValues.toSeq(index)) 
    vars += newVal
    newVal
  }
}

object Val {

  def apply[A](initialValue : A) = new Val[A] {
    var value = initialValue
  }
  
  implicit class RichVal[A](val theVal : Val[A]) extends AnyVal {
    def bind(f : => ValPlaceholder[A] => RenderNode) = 
      new BoundVal[A, A](theVal, Iterable(theVal.get), f)
  }
  
  implicit class RichOptionVal[A](val theVal : Val[_ <: Option[A]]) extends AnyVal {
    def bindAll(f : => ValPlaceholder[A] => RenderNode) = 
      new BoundVal(theVal, theVal.get, f)
  }
  
  implicit class RichIterableVal[A](val theVal : Val[_ <: Iterable[A]]) extends AnyVal {
    def bindAll(f : => ValPlaceholder[A] => RenderNode) = 
      new BoundVal(theVal, theVal.get, f)
  }
}


