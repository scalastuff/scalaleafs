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
package net.scalaleafs

import scala.xml.NodeSeq
import scala.xml.Elem
import java.util.UUID
import java.lang.ref.WeakReference
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

object Placeholder {
  implicit def placeholderValue[A](e : Placeholder[A]) : A = e.get
}

class Placeholder[A] {
  private[scalaleafs] var value : A = null.asInstanceOf[A]
  def get : A = {
    if (value == null) throw new IllegalStateException("Used a placeholder outside rendering")
    value
  }
  override def toString = 
    if (value != null) value.toString else "(undefined)"

  override def hashCode = value.hashCode

  override def equals(other : Any) = value == other
}

/**
 * A Bindable is used to bind data to the render tree. When bound, changes to data causes parts of the tree to 
 * re-rendered. Bindables can be triggered explicitly, which also causes the bound subtree to be re-rendered.
 * Bindables can depend on other bindables, whose changes are also listened to.
 */
trait Versioned { 
  private var _version : Int = 0
  
  /**
   * Triggers this value. All listeners will be triggered as well.
   */
  def trigger() : Unit =
    _version += 1
  
   /**
    * Current version of the bindable. Always >= 0.
    */
  protected [scalaleafs] def version : Int = 
    _version
}

/**
 * Bindable that depends on another bindable.
 */
class MappedVersioned(origin : Versioned) extends Versioned {
  private var _originVersion : Int = 0
  override protected[scalaleafs] def version = {
    val originVersion = origin.version
    if (_originVersion != originVersion) {
      if (validateChange)
        trigger()
      _originVersion = originVersion
    }
    super.version
  } 
  protected def validateChange = true
}

/**
 * Simplest kind of bindable. It holds no data and no function. It can only be triggered explicitly.  
 */
class Trigger extends Versioned 

object Trigger {
  def apply = new Trigger
}

sealed trait Bindable[A] extends Versioned {
  protected[scalaleafs] def version : Int 
  def trigger() : Unit
  def map[B](f : A => B) : Bindable[B]
  def mapAsync[B](f : A => Future[B])(implicit _ec : ExecutionContext) : AsyncVal[B]
  def mapVar[B](f : A => B) : Bindable[B]
}

/**
 * Base trait for bindables that hold a value of type A.
 * A Val cannot be modified. Note, however, that the underlying value
 * of the Val CAN change, in which case re-rendering is triggered.
 */
trait Val[A] extends Bindable[A] { origin =>
  protected[scalaleafs] def get : A

  def map[B](f : A => B) = 
    new MappedVersioned(origin) with Val[B] {
      def get : B = f(origin.get)
    }

  def mapAsync[B](f : A => Future[B])(implicit _ec : ExecutionContext) = 
    new MappedVersioned(origin) with AsyncVal[B] {
      implicit val ec = _ec
      def get : Future[B] = f(origin.get)
    }

  def mapVar[B](f : A => B) = 
    new MappedVersioned(origin) with Var[B] {
      protected var _value : B = f(origin.get)
      override def get : B = _value
      override def validateChange = set(f(origin.get))
    }
}

object Val {
  
  def apply[A](f : => A) = 
    new Val[A] {
      def get : A = f
    }

  def apply[A](f : => Future[A])(implicit _ec : ExecutionContext) = 
    new AsyncVal[A] {
      implicit val ec = _ec
      def get = f
    }
}

/**
 * Asynchronous value.
 */
trait AsyncVal[A] extends Bindable[A] { origin =>
  protected[scalaleafs] implicit val ec : ExecutionContext
  protected[scalaleafs] def get : Future[A]

  def map[B](f : A => B) : AsyncVal[B] = 
    new MappedVersioned(origin) with AsyncVal[B] {
      implicit val ec = origin.ec
      def get : Future[B] = origin.get.map(f)
    }

  def mapAsync[B](f : A => Future[B])(implicit _ec : ExecutionContext) = 
    new MappedVersioned(origin) with AsyncVal[B] {
      val ec = _ec
      def get : Future[B] = origin.get.flatMap(f)  
  }

  def mapVar[B](f : A => B) = 
    map(f)
}

trait Var[A] extends Val[A] {
  protected var _value : A
  def get : A = _value
  def set(value : A) : Boolean = {
    if (_value != value) {
      _value = value
      trigger()
      true
    }
    else false
  }
}

object Var {
  def apply[A](initialValue : A) =  
    new Var[A] {
      protected var _value = initialValue 
    }
}

class WindowVar[A] (val initialValue : A)

object WindowVar {
  implicit def toVar[A](v : WindowVar[A])(implicit context : Context) : Var[A] = 
    context.window.windowVars.getOrElseUpdate(this, Var[A](v.initialValue)).asInstanceOf[Var[A]]
}

class RequestVar[A](val initialValue : A)

object RequestVar {
  implicit def toVar[A](v : RequestVar[A])(implicit context : Context) : Var[A] = 
    context.requestVars.getOrElseUpdate(this, Var[A](v.initialValue)).asInstanceOf[Var[A]]
}

class RequestVal[A]

object RequestVal {
  type Assignment[A] = (RequestVal[A], A)
  implicit def toVar[A](v : RequestVal[A])(implicit context : Context) : Var[A] = 
    context.requestVars.getOrElseUpdate(this, Var[A](null.asInstanceOf[A])).asInstanceOf[Var[A]]
}

trait CurrentUrl
object CurrentUrl extends CurrentUrl {
  implicit def toVar(x : CurrentUrl)(implicit context : Context) : Var[Url] = context.window._currentUrl
}
