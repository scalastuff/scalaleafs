package net.scalaleafs.contrib

import net.scalaleafs.JSCmd;

object Dialogs {

  def confirmation(text : String)(f : => JSCmd) = {
    JSCmd("alert('pos: ' + event.screenX + ', ' + event.screenY'")
  } 
}
