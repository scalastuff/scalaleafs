package net.scalaleafs2

import scala.collection.mutable
import scala.xml.NodeSeq

object Match extends Binding {
  def apply[A](bindable : Val[A])(f : A => RenderNode) = {
    var cache = mutable.HashMap[A, RenderNode]()
    bind(bindable) { placeholder =>
      new RenderNodeDelegate(cache.getOrElseUpdate(placeholder.get, f(placeholder.get)))
    }
  }
  def apply[A](placeholder : Placeholder[A])(f : A => RenderNode) = {
    var cache = mutable.HashMap[A, RenderNode]()
    new RenderNodeDelegate(cache.getOrElseUpdate(placeholder.get, f(placeholder.get)))
  }
}