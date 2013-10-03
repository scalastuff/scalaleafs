package net.scalaleafs2

import implicits._
import scala.xml.NodeSeq

object Placeholder {
  implicit def toA[A](e : Placeholder[A]) : A = e.get
  implicit def toVar[A](e : Placeholder[A]) : Var[A] = e.toVar
}

class Placeholder[A](values : => Seq[A], index : Int, mkVar : Int => Var[A]) {
  def get = values(index)
  lazy val toVar = mkVar(index)
}

