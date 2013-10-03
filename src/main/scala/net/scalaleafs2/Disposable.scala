package net.scalaleafs2

import scala.collection.generic.GenericTraversableTemplate
import scala.collection.generic.TraversableFactory
import scala.collection.TraversableLike

trait Disposable {
  private[scalaleafs2] var next : Option[Disposable] = None
  private[scalaleafs2] var prev : Option[Disposable] = None
  def dispose = {
    if (next.isDefined)
      next.get.prev = prev
    if (prev.isDefined)
      prev.get.next = next
  }
  protected[scalaleafs2] def shouldBeDisposed : Boolean = false
}

class DisposableList[A <: Disposable] extends Disposable {
  
  def add(a : A): A = {
    assert(a.next == None && a.prev == None && prev == None)
    if (next.isDefined) 
      next.get.prev = Some(a)
    a.next = next
    a.prev = Some(this)
    next = Some(a)
    a
  }
  
  def clear {
    next = None
    prev = None
  }
  
  def foreach(f : A => Unit) = {
    var a = next
    while (a.isDefined) {
      val v = a.get
      if (v.shouldBeDisposed) 
        v.dispose
      else 
        f(v.asInstanceOf[A])
      a = v.next
    }
  }
  
  override def toString = {
    val out = new StringBuilder
    foreach { a =>
      if (out.isEmpty) out.append("DisposableList(")
      else out.append(",")
      out.append(a)
    }
    out.append(")")
    out.toString
  }
}

