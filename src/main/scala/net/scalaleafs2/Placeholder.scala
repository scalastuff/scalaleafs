package net.scalaleafs2

import implicits._
import scala.xml.NodeSeq

object Placeholder {
  implicit def toA[A](e : Placeholder[A]) = e.v.get
}

class Placeholder[A](val v : Var[A]) 

object Main extends App {
  val x = "X"
  val t = x.bind { e => 
    println("Running Transformation")
    new RenderNode {
      override def apply(context : Context, xml : NodeSeq) = xml
    }
  }
}