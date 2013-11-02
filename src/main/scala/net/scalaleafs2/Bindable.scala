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
  implicit def toA[A](e : Placeholder[A]) : A = e.get
}

class Placeholder[A] {
  private[scalaleafs2] var value : A = null.asInstanceOf[A]
  def get : A = {
   println("Reading placeholder " + this)
    value
  }
  override def toString = 
    if (value != null) value.toString else "(undefined)"
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
  def version : Int = 
    _version
}

/**
 * Bindable that depends on another bindable.
 */
class MappedBindable(origin : Bindable) extends Bindable {
  private var _originVersion : Int = 0
  override def version = {
    if (_originVersion != origin.version) {
      _originVersion = origin.version
      trigger
    }
    super.version
  } 
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
}

/**
 * Synchronous value.
 */
trait SyncVal[A] extends Val[A] { origin =>
  protected[scalaleafs2] def get(context : Context) : A
  
  def map[B](f : Context => A => B) = 
    new MappedBindable(origin) with SyncVal[B] {
      def get(context : Context) : B = f(context)(origin.get(context))  
    }

  def mapAsync[B](f : Context => A => Future[B]) = 
    new MappedBindable(origin) with AsyncVal[B] {
      def get(context : Context) : Future[B] = f(context)(origin.get(context))
    }
} 

object SyncVal {

  def apply[A](f : Context => A) = 
    new SyncVal[A] {
      def get(context : Context) : A = f(context)
    }
  
  implicit class RichSyncVal[A](val syncVal : SyncVal[A]) extends AnyVal {
    def bind(f : Placeholder[A] => RenderNode) = 
      new SyncBoundRenderNode(syncVal, f)
  }
  implicit class OptionDelegate[A](val bindable : SyncVal[Option[A]]) extends SyncVal[Iterable[A]] {
    override def version = bindable.version
    def get(context : Context) = bindable.get(context)
  }
  implicit class RichOptionSyncVal[A](val bindable : SyncVal[Option[A]]) extends AnyVal {
    def bindAll(f : Placeholder[A] => RenderNode) = 
      new SyncBoundAllRenderNode(bindable, f)
  }
  implicit class RichIterableSyncVal[A](val bindable : SyncVal[_ <: Iterable[A]]) extends AnyVal {
    def bindAll(f : Placeholder[A] => RenderNode) = 
      new SyncBoundAllRenderNode(bindable, f)
  }
}

/**
 * Asynchronous value.
 */
trait AsyncVal[A] extends Val[A] { origin =>
  protected[scalaleafs2] def get(context : Context) : Future[A]

  def map[B](f : Context => A => B) : AsyncVal[B] = 
    new MappedBindable(origin) with AsyncVal[B] {
      def get(context : Context) : Future[B] = origin.get(context).map(f(context))(context.executionContext)
    }

  def mapAsync[B](f : Context => A => Future[B]) = 
    new MappedBindable(origin) with AsyncVal[B] {
      def get(context : Context) : Future[B] = origin.get(context).flatMap(f(context))(context.executionContext)  
  }
}

object AsyncVal {
  
  def apply[A](f : Context => Future[A]) = 
    new AsyncVal[A] {
      def get(context : Context) = f(context)
    }
  
  implicit class RichAsyncVal[A](val asyncVal : AsyncVal[A]) extends AnyVal {
    def bind(f : Placeholder[A] => RenderNode) = 
      new AsyncBoundRenderNode(asyncVal, f)
  }
  implicit class OptionDelegate[A](val asyncVal : AsyncVal[Option[A]]) extends AsyncVal[Iterable[A]] {
    override def version = asyncVal.version
    def get(context : Context) : Future[Iterable[A]] = {
      asyncVal.get(context).map(_.toIterable)(context.executionContext)
    }
  }
  implicit class RichOptionSyncVal[A](val asyncVal : AsyncVal[Option[A]]) extends AnyVal {
    def bindAll(f : Placeholder[A] => RenderNode) = 
      new AsyncBoundAllRenderNode(asyncVal, f)
  }
  implicit class RichIterableSyncVal[A](val asyncVal : AsyncVal[_ <: Iterable[A]]) extends AnyVal {
    def bindAll(f : Placeholder[A] => RenderNode) = 
      new AsyncBoundAllRenderNode(asyncVal, f)
  }
}

class Var[A](initialValue : A) extends SyncVal[A] {
  private var _value : A = initialValue
  protected[scalaleafs2] def get(context : Context) = _value
  def get = _value
  def set(value : A) = {
    if (_value != value) {
      _value = value
      trigger
    }
  }
}

object Var {
  def apply[A](initialValue : A) = 
    new Var[A](initialValue)
}
