package net.scalaleafs.contrib

import net.scalaleafs.JSCmd;

object Dialogs {

  def confirmation(text : String)(f : => JSCmd) = {
    JSCmd("alert('pos: ' + event.screenX + ', ' + event.screenY'")
  } 
}


trait Node
case class Bin(left : Node, right : Node) extends Node
case class Tri(one : Node, two : Node, three : Node) extends Node
case class Leaf(label : String)  extends Node

trait Protocol {
  def product(t : Option[(Node, Node)])
  def product3[A, B, C](t : Option[(A, B, C)])
  def serialize(node : Bin) = product(Bin.unapply(node))
  def serialize(node : Tri) = product3(Tri.unapply(node))
}