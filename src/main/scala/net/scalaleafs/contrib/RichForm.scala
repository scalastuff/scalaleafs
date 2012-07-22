package net.scalaleafs

import scala.xml.Elem
import scala.xml.NodeSeq
import net.scalaleafs._

//object a {
//  import RichForm._
//  def bind = 
//    "#blas" #> new Field(
//        element= "#myInput" #> textbox("")(x=>x),
//        label= "#myInputLabel" #> "",
//        invalid = "#bla" #> AddClass("invalid")
//    )
//    
//}

object RichFormHeadContribution extends JavaScriptResource(classOf[RichForm], "contrib/RichForm.js")

abstract class RichFormElement extends ElemTransformation { t0 =>
  def apply(elem : Elem) : Elem 
  def withLabel(label : String) = new RichFormElement {
    def apply(elem : Elem) : Elem = {
      val elem2 = t0.apply(elem)
      val id = XmlHelpers.getIdOrElse(elem2, "")
      <span class="labeled-input">
        <label for={id}>{label}</label>
        {elem2}
      </span>
    }      
  }
}

trait RichForm 

object RichForm {

  trait Validator {
    val clientSide : Option[(JsExp, FormExecutionTime.Value)]
    val serverSide : Option[(String => Boolean, FormExecutionTime.Value)]
  }

  object NilValidator extends Validator {
    val serverSide = None
    val clientSide = None
  }

  object EmailValidator extends Validator {
    val clientSide = Some(JsExp("value.contains('@')"), FormExecutionTime.OnKey)
    val serverSide = None
  }

  object FormExecutionTime extends Enumeration {
    val OnSubmit = Value
    val OnChange = Value
    val OnKey = Value
  }

  class Field(element : CssTransformation[RichFormElement], label : XmlTransformation, invalid : XmlTransformation) extends XmlTransformation {
    override def apply(xml : NodeSeq) : NodeSeq = xml
  }

  def onclick(f : => JsCmd) : ElemModifier = {
    SetAttr("onclick", R.callback(_ => R.addPostRequestJs(f)).toString + " return false;")
  }
 
  def textbox(text : String, validator : Validator = NilValidator)(setter : String => Unit) = new RichFormElement {
    def apply(elem : Elem) : Elem = {
      R.addHeadContribution(RichFormHeadContribution)
      val callbackId = R.callbackId(map => setter(map.get("").getOrElse(Nil).headOption.getOrElse("")))
      val id = XmlHelpers.getIdOrElse(elem, callbackId)
      elem.label match {
        case "input" => 
          val enableWhenChanged = false;// TODO make an option
            val options = if (enableWhenChanged) "leafs_richform.FormOptions.EnableWhenChanged" else "leafs_richform.FormOptions.None" 
          R.addPostRequestJs(JsCmd("leafs_richform.formInputInit('" + id + "', " + options + ", '" + callbackId + "');"))
          XmlHelpers.setId(XmlHelpers.setAttr(elem, "value", text), id)
        case _ => elem
      }
    }
  }


  def button(setter : => Unit) : ElemTransformation = button(false)(setter)
    
  def button(enableWhenChanged : Boolean = false)(setter : => Unit) = new ElemTransformation {
    def apply(elem : Elem) : NodeSeq = {
      R.addHeadContribution(RichFormHeadContribution)
      // TODO: callback needs be called after replaceHtml!
      val callbackId = R.callbackId(_ => setter)
      val id = XmlHelpers.getIdOrElse(elem, callbackId)
      elem.label match {
          case "input" => 
            val options = if (enableWhenChanged) "leafs_richform.FormOptions.EnableWhenChanged" else "leafs_richform.FormOptions.None" 
            R.addPostRequestJs(JsCmd("leafs_richform.formInputInit('" + id + "', " + options + ", '" + callbackId + "');"))
            XmlHelpers.setId(elem, id)
          case _ => elem
      }
    }
  }
  
  // TODO replace var with set-method
  // TODO implement based on textbox
  def searchbox(textVar : Var[String], defaultText : String = "", defaultClass : String = "default", iconClass : String = "icon", clearLinkClass : String = "clear") = {
     textVar.bind { text =>
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