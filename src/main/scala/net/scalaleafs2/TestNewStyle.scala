package net.scalaleafs2

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

//  implicit def toRenderNode2(template : {def render : RenderNode}) = IdentRenderNode
  
//  implicit def toRenderNode[T <: Template, R <: SyncRenderNode](template : Template) = IdentRenderNode

  class BooksPage(urlTail : Var[List[String]]) extends Template {

    val render : RenderNode = new BooksHeader()
  }
  
  class BooksHeader extends Template {
    
    val render2 = ".hi"  #> mkElem("div")
    val render = ".hi"  #> mkElem("div")
  }
  
  
  class Main extends Template {
    val items = Var(List("X"))
    
    val render : RenderNode = {
      ".hi" #> <hi></hi> &
//      ".hi" #> new BooksPage(items) &
      ".selected" #> items.bind { item =>
        "@href" #> item.toString 
//        & 
//        ".menu" #> new Menu(Var(item + ".")).bind
      }  
    }
  }
}


