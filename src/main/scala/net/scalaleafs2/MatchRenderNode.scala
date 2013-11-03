package net.scalaleafs2

import scala.collection.mutable
import scala.xml.NodeSeq

object Match {
  def apply[A](bindable : Val[A])(f : A => RenderNode) = {
    var cache = mutable.HashMap[A, RenderNode]()
    bindable.bind { placeholder =>
      new HasRenderNode {
        def child = 
            cache.getOrElseUpdate(placeholder.get, f(placeholder.get))
      }
    }
  }
  def apply[A](placeholder : Placeholder[A])(f : A => RenderNode) = {
    var cache = mutable.HashMap[A, RenderNode]()
    new HasRenderNode {
      def child = 
          cache.getOrElseUpdate(placeholder.get, f(placeholder.get))
    }
  }
}