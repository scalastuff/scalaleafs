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

/**
 * Base trait for Var and SeqVar. It implements change-listening and concatenation.
 */
trait Changeable[A] {
  private[this] var listeners = Seq[A => Unit]()
  protected[this] var value : A 
  
  /**
   * Registers a change listener for this var.
   */
  def onChange(f : A => Unit) = listeners = listeners :+ f
  
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
      listeners.foreach(_(value))
    }
  }
    
  /**
   * Modify the current value. It's a combined get and set. Listeners and dependent vars are notified
   * when the value is changed.
   */
  def modify(f : A => A) {
    set(f(value))
  }
  
  protected def dependsOn[B](b : Changeable[B])(f : B => A) : A = {
    b.onChange(bvalue => set(f(bvalue)))
    f(b.get)
  }
  
  protected def dependsOn[B, C](b : Changeable[B], c : Changeable[C])(f : (B, C) => A) : A = {
      b.onChange(bvalue => set(f(bvalue, c.get)))
      c.onChange(cvalue => set(f(b.get, cvalue)))
      f(b.get, c.get)
  }
  
  protected def dependsOn[B, C, D](b : Changeable[B], c : Changeable[C], d : Changeable[D])(f : (B, C, D) => A) : A = {
      b.onChange(bvalue => set(f(bvalue, c.get, d.get)))
      c.onChange(cvalue => set(f(b.get, cvalue, d.get)))
      d.onChange(dvalue => set(f(b.get, c.get, dvalue)))
      f(b.get, c.get, d.get)
  }
  
  protected def dependsOn[B, C, D, E](b : Changeable[B], c : Changeable[C], d : Changeable[D], e : Changeable[E])(f : (B, C, D, E) => A) : A = {
      b.onChange(bvalue => set(f(bvalue, c.get, d.get, e.get)))
      c.onChange(cvalue => set(f(b.get, cvalue, d.get, e.get)))
      d.onChange(dvalue => set(f(b.get, c.get, dvalue, e.get)))
      e.onChange(evalue => set(f(b.get, c.get, d.get, evalue)))
      f(b.get, c.get, d.get, e.get)
  }
}

class CompoundChangeable2[A, B](a : Changeable[A], b : Changeable[B]) { 
  def map[C](f : (A, B) => C) = new Var[C] {
    var value = dependsOn(a, b) (f(_, _))
  }
  def mapSeq[C](f : (A, B) => Iterable[C]) = new SeqVar[C] {
    var value = dependsOn(a, b) (f(_, _).toSeq)
  }
}

class CompoundChangeable3[A, B, C](c1 : Changeable[A], c2 : Changeable[B], c3 : Changeable[C]) {
  def map[D](f : (A, B, C) => D) = new Var[D] {
    var value = dependsOn(c1, c2, c3)(f(_, _, _))
  }
  def mapSeq[D](f : (A, B, C) => Iterable[D]) = new SeqVar[D] {
    var value = dependsOn(c1, c2, c3)(f(_, _, _).toSeq)
  }
}

class CompoundChangeable4[A, B, C, D](c1 : Changeable[A], c2 : Changeable[B], c3 : Changeable[C], c4 : Changeable[D]) {
  def map[E](f : (A, B, C, D) => E) = new Var[E] {
    var value = dependsOn(c1, c2, c3, c4)(f(_, _, _, _))
  }
  def mapSeq[E](f : (A, B, C, D) => Iterable[E]) = new SeqVar[E] {
    var value = dependsOn(c1, c2, c3, c4)(f(_, _, _, _).toSeq)
  }
}

trait OptionVarWithToSeq[A] { thisVar : Var[Option[A]] =>
  def toSeq = new SeqVar[A] {
    var value = dependsOn(thisVar)(_.toSeq)
  }
}

trait IterableVarWithToSeq[A, B <: Iterable[A]] { thisVar : Var[B] =>
  def toSeq = new SeqVar[A] {
    var value = dependsOn(thisVar)(_.toSeq)
  }
}

object Var {
  implicit def fromInitialIterableValue[A, B <: Iterable[A]](initialValue : B) : IterableVarWithToSeq[A, B] = apply(initialValue)
  implicit def fromInitialValue[A](initialValue : A) : Var[A] = apply(initialValue)
  implicit def fromInitialOptionValue[A](initialValue : Option[A]) : OptionVarWithToSeq[A] = apply(initialValue)
  
  def apply[A](initialValue : Option[A]) = new Var[Option[A]] with OptionVarWithToSeq[A] {
    var value = initialValue
  }
  def apply[A, B <: Iterable[A]](initialValue : B) = new Var[B] with IterableVarWithToSeq[A, B] {
    var value = initialValue
  }
  def apply[A](initialValue : A) = new Var[A] {
    var value = initialValue
  } 
}

/**
 * A Var is a wrapper for a mutable value. One can listen for changes, or map a Var to other Vars that become
 * dependent on it. Vars can also be rendered, transforming XML based on the current value of the Var. When
 * the Var changes (or one of the Vars it depends on), the transformation is performed again. The piece of 
 * XML that was created by the transformation is sent to the browser using a ReplaceHtml JavaScript command.
 */
trait Var[A] extends Changeable[A] { thisVar =>
  
  /**
   * Maps this var to another one.
   */
  def map[B](f : A => B) = new Var[B] {
    var value = dependsOn(thisVar)(f(_))
  }
  
  /**
   * Maps this var to a seq-var.
   */
  def mapSeq[B](f : A => Iterable[B]) = new SeqVar[B] {
    var value = dependsOn(thisVar)(f(_).toSeq)
  }
  def zipWith[B](values : SeqVar[B]) = new SeqVar[(A, B)] {
    var value = dependsOn(thisVar, values)((avalue, bvalues) => bvalues.map(bvalue => (avalue, bvalue)))
  }
  def zipWith[B](values : Option[B]) = new SeqVar[(A, B)] {
    var value = dependsOn(thisVar)(avalue => values.toSeq.map(bvalue => (avalue, bvalue)))
  }
  def zipWith[B](values : Iterable[B]) = new SeqVar[(A, B)] {
    var value = dependsOn(thisVar)(avalue => values.toSeq.map(bvalue => (avalue, bvalue)))
  }
  /**
   * Bind the var to the XML output. When the value of the var is changed, the XML output is
   * re-rendered and sent to the browser. In the browser, the specific section that was
   * created by this bind, will be replaced by the new XML output.
   * 
   * @param f Transformation that renders the value of the var.
   */
  def render(f : A => NodeSeq => NodeSeq) = new ElemWithIdTransformation {
    override def apply(elem : Elem, id : String) = {
      onChange(value => {
        R.addEagerPostRequestJs(ReplaceHtml(id, f(value)(elem)))
      })
      f(value)(elem)
    }
  }
}

object SeqVar {
  def apply[A](initialValue : Option[A]) = new SeqVar[A] {
    var value = initialValue.toSeq
  }
  def apply[A](initialValue : Iterable[A]) = new SeqVar[A] {
    var value = initialValue.toSeq
  }
}

trait SeqVar[A] extends Changeable[Seq[A]] { thisVar =>
  def map[B](f : A => B) = new SeqVar[B] {
    var value = dependsOn(thisVar)(_.map(f))
  }
  def zipWith[B](other : B) = new SeqVar[(A, B)] {
    var value = dependsOn(thisVar)(_.map((_, other)))
  }
  
  def modify2(pf : PartialFunction[A, A]) {
    set {
      value.map { v =>
        if (pf.isDefinedAt(v)) pf(v) else v
      }
    }
  }
  
  /**
   * Bind the SeqVar to the XML output. When the value of the var is changed, the XML output is
   * re-rendered and sent to the browser. In the browser, the specific section that was
   * created by this bind, will be replaced by the new XML output.
   * 
   * @param empty Transformation to render the 'no elements available' case.
   * @param f Transformation that renders a single sequence element.
   */
  def render(empty : NodeSeq => NodeSeq)(f : A => NodeSeq => NodeSeq) = new ElemWithIdTransformation {
    override def apply(elem : Elem, id : String) = {
      def gen(values : Seq[A]) = 
        if (values.isEmpty) {
          empty(elem)
        } else {
          values.zipWithIndex.flatMap {
            case (value, 0) => f(value)(elem)
            case (value, index) => f(value)(XmlHelpers.setId(elem, id + "-" + index))
          }
        }
        
      thisVar.onChange(avalue => R.addEagerPostRequestJs(RemoveNextSiblings(id, id) & ReplaceHtml(id, gen(avalue))))
      gen(value)
    }
  }
}


