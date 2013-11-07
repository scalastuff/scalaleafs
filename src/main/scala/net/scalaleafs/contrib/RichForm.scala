/**
 * Copyright (c) 2012 Ruud Diterwich.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package net.scalaleafs.contrib

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

object RichFormHeadContribution extends JavaScriptResource(classOf[RichForm], "RichForm.js")

abstract class RichFormElement extends ElemModifier with NoChildRenderNode { t0 =>
  def withLabel(label : String) = new RichFormElement {
    def render(context : Context, elem : Elem) : Elem = {
      val elem2 = t0.render(context, elem)
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
    val clientSide : Option[(JSExp, FormExecutionTime.Value)]
    val serverSide : Option[(String => Boolean, FormExecutionTime.Value)]
  }

  object NilValidator extends Validator {
    val serverSide = None
    val clientSide = None
  }

  object EmailValidator extends Validator {
    val clientSide = Some(JSExp("value.contains('@')"), FormExecutionTime.OnKey)
    val serverSide = None
  }

  object FormExecutionTime extends Enumeration {
    val OnSubmit = Value
    val OnChange = Value
    val OnKey = Value
  }

  class Field(element : RenderNode, label : RenderNode, invalid : RenderNode) extends NoChildRenderNode {
    def render(context : Context, xml : NodeSeq) : NodeSeq = xml
  }

  def onclick(f : => JSCmd) : ElemModifier = {
    Xml.setAttr("onclick", _.callback(context => _ => context.addPostRequestJs(f)).toString + " return false;")
  }
 
  def textbox(text : String, validator : Validator = NilValidator)(setter : String => Unit) = new RichFormElement {
    def render(context : Context, elem : Elem) : Elem = {
      context.addHeadContribution(RichFormHeadContribution)
      val callbackId = context.callbackId(_ => map => setter(map.get("").getOrElse(Nil).headOption.getOrElse("")))
      val id = XmlHelpers.getIdOrElse(elem, callbackId)
      elem.label match {
        case "input" => 
          val enableWhenChanged = false;// TODO make an option
            val options = if (enableWhenChanged) "leafs_richform.FormOptions.EnableWhenChanged" else "leafs_richform.FormOptions.None" 
          context.addPostRequestJs(JSCmd("leafs_richform.formInputInit('" + id + "', " + options + ", '" + callbackId + "');"))
          XmlHelpers.setId(XmlHelpers.setAttr(elem, "value", text), id)
        case _ => elem
      }
    }
  }


  def button(setter : => Unit) : ElemModifier = button(false)(setter)
    
  def button(enableWhenChanged : Boolean = false)(setter : => Unit) = new ElemModifier with NoChildRenderNode {
    def render(context : Context, elem : Elem) : Elem = {
      context.addHeadContribution(RichFormHeadContribution)
      // TODO: callback needs be called after replaceHtml!
      val callbackId = context.callbackId(_ => _ => setter)
      val id = XmlHelpers.getIdOrElse(elem, callbackId)
      elem.label match {
          case "input" => 
            val options = if (enableWhenChanged) "leafs_richform.FormOptions.EnableWhenChanged" else "leafs_richform.FormOptions.None" 
            context.addPostRequestJs(JSCmd("leafs_richform.formInputInit('" + id + "', " + options + ", '" + callbackId + "');"))
            XmlHelpers.setId(elem, id)
          case _ => elem
      }
    }
  }
  
  // TODO replace var with set-method
  // TODO implement based on textbox
//  def searchbox(textVar : Var[String], defaultText : String = "", defaultClass : String = "default", iconClass : String = "icon", clearLinkClass : String = "clear") = {
//     textVar.bind { text =>
//       Xml.addClass(defaultClass, text == "") & 
//       Xml.setContent { _ =>
//         <span class={iconClass}/> ++
//         <a class={clearLinkClass} 
//           onclick={R.callback(_ => textVar.set("")) & JsReturnFalse}> </a> ++
//         <input 
//           value={if (text == "") defaultText else text} 
//           onfocus={"if (this.value == '" + defaultText + "') { this.value = ''; leafs.removeClass(this.parentNode, '" + defaultClass + "') }"} 
//           onBlur={
//             R.callback("value" -> JSExp("this.value"))(s => textVar.set(s("value").mkString)) + " return false;" +
//             "if (this.value == '') { leafs.addClass(this.parentNode, '" + defaultClass + "'); this.value = '" + defaultText + "' } "
//             } 
//           />
//       }
//     }
// }
}