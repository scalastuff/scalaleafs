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

object Placeholder {
  implicit def placeholderValue[A](e : Placeholder[A]) : A = e.get
}

class Placeholder[A] {
  private[scalaleafs2] var value : A = null.asInstanceOf[A]
  def get : A = {
    if (value == null) throw new IllegalStateException("Used a placeholder outside rendering")
    value
  }
  override def toString = 
    if (value != null) value.toString else "(undefined)"
      
  override def equals(other : Any) = value == other
}

/**
 * A Bindable is used to bind data to the render tree. When bound, changes to data causes parts of the tree to 
 * re-rendered. Bindables can be triggered explicitly, which also causes the bound subtree to be re-rendered.
 * Bindables can depend on other bindables, whose changes are also listened to.
 */
trait Bindable { 
  private var _version : Int = 0
  
  /**
   * Triggers this value. All listeners will be triggered as well.
   */
  def trigger : Unit = 
    _version += 1
  
   /**
    * Current version of the bindable. Always >= 0.
    */
  private[scalaleafs2] def version(context : Context) : Int = 
    _version
}

/**
 * Bindable that depends on another bindable.
 */
class MappedBindable(origin : Bindable) extends Bindable {
  private var _originVersion : Int = 0
  override def version(context : Context) = {
    val originVersion = origin.version(context)
    if (_originVersion != originVersion) {
      if (validateChange(context))
        trigger
      _originVersion = originVersion
    }
    super.version(context)
  } 
  protected def validateChange(context : Context) = true
}

/**
 * Simplest kind of bindable. It holds no data and no function. It can only be triggered explicitly.  
 */
class Trigger extends Bindable 

object Trigger {
  def apply = new Trigger
}

/**
 * Base trait for bindables that hold a value of type A.
 * A Val cannot be modified. Note, however, that the underlying value
 * of the Val CAN change, in which case re-rendering is triggered.
 */
trait Val[A] extends Bindable {
  def map[B](f : Context => A => B) : Val[B]
  def mapAsync[B](f : Context => A => Future[B]) : Val[B]
  def mapVar[B](f : Context => A => B) : Val[B]
  def isAsync : Boolean 
  protected[scalaleafs2] def get(context : Context) : A
  protected[scalaleafs2] def getAsync(context : Context) : Future[A]

}

object Val {
  implicit class RichVal[A](val bindable : Val[A]) extends AnyVal {
    def bind(f : Placeholder[A] => RenderNode) =
      new BoundRenderNode(bindable, f)
  }
  implicit class OptionDelegate[A](val bindable : Val[Option[A]]) extends SyncVal[Iterable[A]] {
    override def version(context : Context) = bindable.version(context)
    def get(context : Context) = bindable.get(context)
  }
  implicit class RichOptionVal[A](val bindable : Val[Option[A]]) extends AnyVal {
    def bindAll(f : Placeholder[A] => RenderNode) = 
      new BoundAllRenderNode(bindable, f)
  }
  implicit class RichIterableVal[A](val bindable : Val[_ <: Iterable[A]]) extends AnyVal {
    def bindAll(f : Placeholder[A] => RenderNode) = 
      new BoundAllRenderNode(bindable, f)
  }
  implicit class RichUrlVal(val v : Val[Url]) extends AnyVal {
    def head = v.mapVar(_ => _.head)
    def headOption = v.mapVar(_ => _.headOption)
    def tail  = v.mapVar(_ => _.tail)
  }
}
  
/**
 * Synchronous value.
 */
trait SyncVal[A] extends Val[A] { origin =>
  protected[scalaleafs2] def getAsync(context : Context) = ???
  def isAsync = false
  
  def map[B](f : Context => A => B) = 
    new MappedBindable(origin) with SyncVal[B] {
      def get(context : Context) : B = f(context)(origin.get(context))  
    }

  def mapAsync[B](f : Context => A => Future[B]) = 
    new MappedBindable(origin) with AsyncVal[B] {
      def getAsync(context : Context) : Future[B] = f(context)(origin.get(context))
    }

  def mapVar[B](f : Context => A => B) = 
    new MappedBindable(origin) with Var[B] {
      override def validateChange(context : Context) = set(f(context)(origin.get(context)))
    }
} 

object SyncVal {

  def apply[A](f : Context => A) = 
    new SyncVal[A] {
      def get(context : Context) : A = f(context)
    }
}

/**
 * Asynchronous value.
 */
trait AsyncVal[A] extends Val[A] { origin =>
  protected[scalaleafs2] def get(context : Context) : A = ???
  def isAsync = true

  def map[B](f : Context => A => B) : AsyncVal[B] = 
    new MappedBindable(origin) with AsyncVal[B] {
      def getAsync(context : Context) : Future[B] = origin.getAsync(context).map(f(context))(context.executionContext)
    }

  def mapAsync[B](f : Context => A => Future[B]) = 
    new MappedBindable(origin) with AsyncVal[B] {
      def getAsync(context : Context) : Future[B] = origin.getAsync(context).flatMap(f(context))(context.executionContext)  
  }

  def mapVar[B](f : Context => A => B) = 
    map(f)
}

object AsyncVal {
  
  def apply[A](f : Context => Future[A]) = 
    new AsyncVal[A] {
      def getAsync(context : Context) = f(context)
    }
}

trait Var[A] extends SyncVal[A] {
  private var _value : A = null.asInstanceOf[A]
  protected[scalaleafs2] def get(context : Context) = _value
  def get = _value
  def set(value : A) : Boolean = {
    if (_value != value) {
      _value = value
      trigger
      true
    }
    else false
  }
}


object Var {
  def apply[A](initialValue : A) =  
    new Var[A] {
     set(initialValue)
    }
}
