package org.scalastuff.scalaleafs

import scala.xml.Elem
import scala.xml.NodeSeq
import implicits._

object Html {
 
  def onclick(f : => JsCmd) : ElemModifier = {
    SetAttr("onclick", R.callback(_ => R.addPostRequestJs(f)).toString + " return false;")
  }
 
  def editBox(label : Option[String], text : String)(setter : String => Unit) = new ElemTransformation {
    def apply(elem : Elem) : NodeSeq = {
      val callbackId = R.callbackId(map => setter(map.get("").getOrElse(Nil).headOption.getOrElse("")))
      val id = XmlHelpers.getIdOrElse(elem, callbackId)
      val elem2 = elem.label match {
        case "input" => 
          R.addPostRequestJs(JsCmd("leafs.formInputInit('" + id + "', 0, '" + callbackId + "');"))
          XmlHelpers.setId(XmlHelpers.setAttr(elem, "value", text), id)
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
  
  // TODO replace var with set method
  // TODO implement based on form textbox
  def searchBox(textVar : Var[String], defaultText : String = "", defaultClass : String = "default", iconClass : String = "icon", clearLinkClass : String = "clear") = {
     textVar.render { text =>
       AddClass(defaultClass, text == "") & 
       SetContent { _ =>
         <span class={iconClass}/> ++
         <a class={clearLinkClass} 
           onclick={R.callback(_ => textVar.set("")) & JsReturnFalse}> </a> ++
         <input 
           value={if (text == "") defaultText else text} 
           onfocus={"if (this.value == '" + defaultText + "') { this.value = ''; leafs.removeClass(this.parentNode, '" + defaultClass + "') }"} 
           onBlur={
             R.callback(s => textVar.set(s("value").mkString), "value" -> JsExp("this.value")) + " return false;" +
             "if (this.value == '') { leafs.addClass(this.parentNode, '" + defaultClass + "'); this.value = '" + defaultText + "' } "
             } 
           />
       }
     }
 }
}