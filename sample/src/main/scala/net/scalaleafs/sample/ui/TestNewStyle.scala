package net.scalaleafs.sample.ui

import scala.xml.NodeSeq
import scala.xml.Text
import net.scalaleafs2._
import net.scalaleafs2.implicits._


class X

trait T {
  implicit def authenticatedUser
  def reg[A](f : (Context => X) => A)
 
  def doit(context : Context)(implicit x : X) = {
    println(x)
  }
  
}

object TestNewStyle {

  def Case(f : => Boolean)(t : RenderNode) = 
    new Case(f)(t)
    
  class Case(f : => Boolean)(t : RenderNode) extends RenderNode {
    def Case(f : => Boolean)(t : RenderNode) {
      
    }
  }
  
  
  
  class Main extends Template {
    val items = Var(List("X"))
    
    
    val bind : RenderNode = {
      ".hi" #> <hi></hi> &
      ".selected" #> items.bind { item =>
        "@href" #> item.toString & 
        ".menu" #> new Menu(Var(item + ".")).bind
      } & 
      "selected" #> {
        "#a" #>
        Case("" == "") {
          new Menu(null)
           ".hi" #> <hi></hi>
        } 
        Case(false) {
           ".hi" #> <hi></hi>
        }
      }
    }
  }
  class Menu(item : Var[String]) extends Template {
    val bind : RenderNode = ""
  }
}


