package org.scalastuff.scalaleafs

import scala.xml.Elem
import scala.xml.NodeSeq
import implicits._

object Widgets {
  def onclick(elem : Elem, f : => JsCmd) : Elem = {
    XmlHelpers.setAttr(elem, "onclick", R.callback(_ => R.addPostRequestJs(f)).toString + " return false;")
  }
 def onclick(f : => JsCmd) : ElemModifier = {
    SetAttr("onclick", R.callback(_ => R.addPostRequestJs(f)).toString + " return false;")
  }
 
 
 def searchBox(textVar : Var[String], defaultText : String = "", defaultClass : String = "default", iconClass : String = "icon", clearLinkClass : String = "clear") = {
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