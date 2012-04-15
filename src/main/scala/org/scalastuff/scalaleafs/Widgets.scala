package org.scalastuff.scalaleafs
import scala.xml.NodeSeq
import scala.xml.Elem
import scala.xml.Attribute
import scala.xml.Text
import scala.xml.Null

object Widgets {
  def onclick_=(f : => JsCmd) : NodeSeq => NodeSeq = onclick(f)
 def onclick(f : => JsCmd) : NodeSeq => NodeSeq = _ match {
    case elem : Elem => 
      val uid = R.registerAjaxCallback { _ =>
        R.addPostRequestJs(f)
      }
      elem % Attribute("onclick", Text("callback('" + uid.toString + "'); return false;"), Null)
    case node => node
  }
}