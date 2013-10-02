package net.scalaleafs.sample.ui

import scala.xml.NodeSeq
import scala.xml.Text
import net.scalaleafs2._
import net.scalaleafs2.implicits._

object TestNewStyle {

  def Case(f : => Boolean)(t : Transformation) = 
    new Case(f)(t)
    
  class Case(f : => Boolean)(t : Transformation) extends Transformation {
    def Case(f : => Boolean)(t : Transformation) {
      
    }
  }
  
  class Main extends Template {
    val items = Seq("AA", "BB")
    
    
    val bind : Transformation = {
      ".hi" #> <hi></hi> &
      ".selected" #> items.bind { item =>
        "@href" #> item.mkString(",") & 
        ".menu" #> new Menu(Var(item + ".")).bind
      } & 
      "selected" #> {
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
    val bind : Transformation = ""
  }
}


