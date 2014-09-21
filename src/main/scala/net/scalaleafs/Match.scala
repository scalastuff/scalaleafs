package net.scalaleafs

import scala.collection.mutable
import scala.xml.NodeSeq

object Match {
  def apply[A](bindable : Val[A])(f : A => RenderNode) = {
    var cache = mutable.HashMap[A, RenderNode]()
    Binding.bind(bindable) { placeholder =>
      new RenderNodeDelegate(cache.getOrElseUpdate(placeholder.get, f(placeholder.get)))
    }
  }
  def apply[A](placeholder : Placeholder[A])(f : A => RenderNode) = {
    var cache = mutable.HashMap[A, RenderNode]()
    new RenderNodeDelegate(cache.getOrElseUpdate(placeholder.get, f(placeholder.get)))
  }
}