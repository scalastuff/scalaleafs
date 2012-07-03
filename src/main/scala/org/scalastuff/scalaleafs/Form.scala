package org.scalastuff.scalaleafs

import scala.xml.NodeSeq
import scala.xml.Elem
import java.util.UUID
import implicits._

object BindForm {
    
  def apply(f : Form => Elem => NodeSeq) = new ElemTransformation {
    override def apply(elem : Elem) : NodeSeq = {
      val form = new Form
      f(form)(XmlHelpers.replaceContent(XmlHelpers.setAttr(elem, "id", form.id), _ ++ 
          <script type="text/javascript">
      var form_{form.id} = new Object();
    </script>))
    }
  }
}

class Form {
  
  val id = UUID.randomUUID.toString.replace('-', '_')
  
  def editBox = new ElemTransformation {
    def apply(elem : Elem) : NodeSeq = {
      val inputId = R.callbackId(_=>Unit)
      XmlHelpers.setAttr(elem, "type", "text")
      XmlHelpers.setAttr(elem, "onchange", "form_" + id + "." + inputId + "=this.value;console.log(form_" + id + ");")
    }
  }
}

object AjaxForm {
 
  def editBox(label : Option[String], text : String)(setter : String => Unit) = new ElemTransformation {
    def apply(elem : Elem) : NodeSeq = {
      val callbackId = R.callbackId(map => setter(map.get("").getOrElse(Nil).headOption.getOrElse("")))
      val id = XmlHelpers.getIdOrElse(elem, callbackId)
      val elem2 = elem.label match {
        case "input" => 
          R.addPostRequestJs(JsCmd("leafs.formInputInit('" + id + "', 0, '" + callbackId + "');"))
          XmlHelpers.setId(XmlHelpers.setAttr(XmlHelpers.setAttr(elem, "value", text), "type", "text"), id)
        case _ => elem
      }
      label match {
        case Some(label) =>
          <span class="labeled-input">
            <label for={id}>{label}</label>
            {elem2}
          </span>
        case None => elem2
      }
    }
  }

  def editBox(text : String)(setter : String => Unit) : ElemTransformation = 
    editBox(None, text)(setter)

  def editBox(label : String, text : String)(setter : String => Unit) : ElemTransformation = 
    editBox(Some(label), text)(setter)

    def button(setter : => Unit) : ElemTransformation = button(false)(setter)
    
  def button(enableWhenChanged : Boolean = false)(setter : => Unit) = new ElemTransformation {
    def apply(elem : Elem) : NodeSeq = {
      // TODO: callback needs be called after replaceHtml!
      val callbackId = R.callbackId(_ => setter)
      val id = XmlHelpers.getIdOrElse(elem, callbackId)
      elem.label match {
          case "input" => 
            val options = if (enableWhenChanged) "leafs.FormOptions.EnableWhenChanged" else "leafs.FormOptions.Nonde" 
            R.addPostRequestJs(JsCmd("leafs.formInputInit('" + id + "', " + options + ", '" + callbackId + "');"))
            XmlHelpers.setId(elem, id)
          case _ => elem
      }
    }
  }
}