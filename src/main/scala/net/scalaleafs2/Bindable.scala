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

trait MappedBindable extends Bindable {
  def origin : Bindable
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

trait Val[A] extends Bindable

class AsyncValMagnet[A, B](val apply: AsyncVal[A] => AsyncVal[B]) extends AnyVal

object AsyncValMagnet {
  implicit def magnet1[A, B](f : A => Future[B]) = 
    new AsyncValMagnet((bindable : AsyncVal[A]) =>
      new MappedAsyncDef(bindable, (context: Context) => bindable.getAsync(context).flatMap(f)(context.executionContext)))
  
  implicit def magnet2[A, B](f : A => B) = 
    new AsyncValMagnet((bindable : AsyncVal[A]) =>
      new MappedAsyncDef(bindable, (context: Context) => bindable.getAsync(context).map(f)(context.executionContext)))
  
  implicit def magnet3[A, B](f : Context => A => Future[B]) = 
    new AsyncValMagnet((bindable : AsyncVal[A]) =>
      new MappedAsyncDef(bindable, (context: Context) => bindable.getAsync(context).flatMap(f(context))(context.executionContext)))
  
  implicit def magnet4[A, B](f : Context => A => B) = 
    new AsyncValMagnet((bindable : AsyncVal[A]) =>
      new MappedAsyncDef(bindable, (context: Context) => bindable.getAsync(context).map(f(context))(context.executionContext)))
} 

trait AsyncVal[A] extends Val[A] {
  protected[scalaleafs2] def getAsync(context : Context) : Future[A]

  def map[B](magnet : AsyncValMagnet[A, B]) =
    magnet.apply(this)
}

object AsyncVal {
  implicit class RichSyncVal[A](val bindable : AsyncVal[A]) extends AnyVal {
    def bind(f : Placeholder[A] => SyncRenderNode) = 
      new BoundRenderNode(bindable, bindable.getAsync, f)
  }
  implicit class OptionDelegate[A](val bindable : AsyncVal[Option[A]]) extends AsyncVal[Iterable[A]] {
    override def version = bindable.version
    def getAsync(context : Context) = {
      import context.executionContext
      bindable.getAsync(context).map(x => x)
    }
  }
  implicit class RichOptionSyncVal[A](val bindable : AsyncVal[Option[A]]) extends AnyVal {
    def bindAll(f : Placeholder[A] => SyncRenderNode) = 
      new BoundAllRenderNode(bindable, f)
  }
  implicit class RichIterableSyncVal[A](val bindable : AsyncVal[_ <: Iterable[A]]) extends AnyVal {
    def bindAll(f : Placeholder[A] => SyncRenderNode) = 
      new BoundAllRenderNode(bindable, f)
  }
}

class SyncValMapMagnet[A, B, S](val apply: SyncVal[A] => S) extends AnyVal

object SyncValMapMagnet {
//  implicit def magnet1[A, B](f : A => Future[B]) = 
//    new SyncValMapMagnet((bindable : SyncVal[A]) => 
//      new MappedAsyncDef(bindable, context => f(bindable.get(context))))
//
//  implicit def magnet2[A, B](f : A => B) = 
//    new SyncValMapMagnet((bindable : SyncVal[A]) => 
//      new MappedSyncDef(bindable, context => f(bindable.get(context))))

  implicit def magnet3[A, B](f : Context => A => Future[B]) = 
    new SyncValMapMagnet((bindable : SyncVal[A]) => 
      new MappedAsyncDef(bindable, context => f(context)(bindable.get(context))))

//  implicit def magnet4[A, B](f : Context => A => B) = 
//    new SyncValMapMagnet((bindable : SyncVal[A]) => 
//      new MappedSyncDef(bindable, context => f(context)(bindable.get(context))))
} 

class SyncValBindMagnet[A, R](val apply : SyncVal[A] => R)
object SyncValBindMagnet {
//  implicit def magnet1[A, B](f : Placeholder[A] => SyncRenderNode) = 
//    new SyncValBindMagnet((bindable : SyncVal[A]) =>
//      new BoundSyncRenderNode(bindable, bindable.get, f))

  implicit def magnet2[A, B](f : Placeholder[A] => RenderNode) = 
    new SyncValBindMagnet((bindable : SyncVal[A]) => 
      new BoundRenderNode(bindable, context => Future.successful(bindable.get(context)), f))
} 


trait SyncVal[A] extends Val[A] {
  protected[scalaleafs2] def get(context : Context) : A
  
  def map[B](f : Context => A => Future[B]) = 
      new MappedAsyncDef(this, context => f(context)(get(context)))
      
//  def map[B, S](magnet : SyncValMapMagnet[A, B, S]) = 
//    magnet.apply(this)
    
//  def bind[R](magnet : SyncValBindMagnet[A, R]) : R = 
//    magnet.apply(this)
} 

object SyncVal {

  implicit class RichSyncVal[A](val bindable : SyncVal[A]) extends AnyVal {
    implicit def b = bindable
//    def bind(f : Placeholder[A] => SyncRenderNode) = 
//      new BoundSyncRenderNode(bindable, bindable.get, f)
    def bind(f : Placeholder[A] => RenderNode) = 
      new BoundRenderNode(bindable, context => Future.successful(bindable.get(context)), f)
  }
  implicit class OptionDelegate[A](val bindable : SyncVal[Option[A]]) extends SyncVal[Iterable[A]] {
    override def version = bindable.version
    def get(context : Context) = bindable.get(context)
  }
  implicit class RichOptionSyncVal[A](val bindable : SyncVal[Option[A]]) extends AnyVal {
    def bindAll(f : Placeholder[A] => SyncRenderNode) = 
      new BoundAllSyncRenderNode(bindable, f)
  }
  implicit class RichIterableSyncVal[A](val bindable : SyncVal[_ <: Iterable[A]]) extends AnyVal {
    def bindAll(f : Placeholder[A] => SyncRenderNode) = 
      new BoundAllSyncRenderNode(bindable, f)
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

class MappedAsyncDef[A](val origin : Bindable, f : Context => Future[A]) extends AsyncVal[A] with MappedBindable {
  protected[scalaleafs2] def getAsync(context : Context) : Future[A] = f(context)
}

class MappedSyncDef[A](val origin : Bindable, f : Context => A) extends SyncVal[A] with MappedBindable {
  protected[scalaleafs2] def get(context : Context) : A = f(context)
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
