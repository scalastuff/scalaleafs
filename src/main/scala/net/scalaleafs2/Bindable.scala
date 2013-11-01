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
 * A Bindable is a wrapper for a value. One can listen for changes, or map a Bindable to other Bindables that become
 * dependent on it. Bindables can also be bound to a XML, transforming XML based on the current value of the Bindable. When
 * the is triggered (or one of the Bindables it depends on), the transformation is performed again. The piece of 
 * XML that was created by the transformation is sent to the browser using a ReplaceHtml JavaScript command.
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

trait Trigger extends Bindable {
}

trait Val[A] extends Bindable {
  def map[B](f : Context => A => B) : Val[B]
  def mapAsync[B](f : Context => A => Future[B]) : Val[B]
}

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

  implicit class RichSyncVal[A](val syncVal : SyncVal[A]) extends AnyVal {
    def bind(f : Placeholder[A] => RenderNode) = 
      new BoundSyncRenderNode(syncVal, context => syncVal.get(context), f)
  }
  implicit class OptionDelegate[A](val bindable : SyncVal[Option[A]]) extends SyncVal[Iterable[A]] {
    override def version = bindable.version
    def get(context : Context) = bindable.get(context)
  }
  implicit class RichOptionSyncVal[A](val bindable : SyncVal[Option[A]]) extends AnyVal {
    def bindAll(f : Placeholder[A] => RenderNode) = 
      new BoundAllSyncRenderNode(bindable, f)
  }
  implicit class RichIterableSyncVal[A](val bindable : SyncVal[_ <: Iterable[A]]) extends AnyVal {
    def bindAll(f : Placeholder[A] => RenderNode) = 
      new BoundAllSyncRenderNode(bindable, f)
  }
}

trait AsyncVal[A] extends Val[A] { origin =>
  protected[scalaleafs2] def get(context : Context) : Future[A]

  def map[B](f : Context => A => B) : AsyncVal[B] = 
    new MappedBindable(origin) with AsyncVal[B] {
      def get(implicit context : Context) : Future[B] = origin.get(context).map(f(context))  
    }

  def mapAsync[B](f : Context => A => Future[B]) = 
    new MappedBindable(origin) with AsyncVal[B] {
      def get(implicit context : Context) : Future[B] = origin.get(context).flatMap(f(context))  
  }
}

object AsyncVal {
  implicit class RichSyncVal[A](val asyncVal : AsyncVal[A]) extends AnyVal {
    def bind(f : Placeholder[A] => RenderNode) = 
      new BoundRenderNode(asyncVal, asyncVal.get, f)
  }
  implicit class OptionDelegate[A](val asyncVal : AsyncVal[Option[A]]) extends AsyncVal[Iterable[A]] {
    override def version = asyncVal.version
    def getAsync(implicit context : Context) = {
      asyncVal.get(context).map(x => x)
    }
  }
  implicit class RichOptionSyncVal[A](val asyncVal : AsyncVal[Option[A]]) extends AnyVal {
    def bindAll(f : Placeholder[A] => RenderNode) = 
      new BoundAllRenderNode(asyncVal, f)
  }
  implicit class RichIterableSyncVal[A](val asyncVal : AsyncVal[_ <: Iterable[A]]) extends AnyVal {
    def bindAll(f : Placeholder[A] => RenderNode) = 
      new BoundAllRenderNode(asyncVal, f)
  }
}

class AsyncDef[A](f : Context => Future[A]) extends AsyncVal[A] {
  def getAsync(context : Context) = f(context)
}

class SyncDef[A](f : Context => A) extends SyncVal[A] {
  def get(context : Context) = f(context)
}

object Def {
  def apply[A](f : Context => Future[A]) = 
    new AsyncDef(f)
  
  def apply[A](f : Context => A) = 
    new SyncDef(f)
  
  def apply[A](f : Future[A]) = 
    new AsyncDef(context => f)
  
  def apply[A](f : A) = 
    new SyncDef(context => f)
}

class Var[A](initialValue : A) extends SyncVal[A] {
  private var _value : A = initialValue
  protected[scalaleafs2] def get(context : Context) = _value
  def get = 
    _value
  def set(value : A) = {
    if (_value != value) {
      println("CHANGED: WAS " + _value + " new " + value)
      _value = value
      trigger
    }
  }
}

object Var {
  def apply[A](initialValue : A) = 
    new Var[A](initialValue)
}
