package net.scalaleafs.contrib

import net.scalaleafs.JsCmd;

object Dialogs {

  def confirmation(text : String)(f : => JsCmd) = {
    JsCmd("alert('pos: ' + event.screenX + ', ' + event.screenY'")
  } 
}
