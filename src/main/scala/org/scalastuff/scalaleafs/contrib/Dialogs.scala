package org.scalastuff.scalaleafs.contrib

import org.scalastuff.scalaleafs.R
import org.scalastuff.scalaleafs.JsCmd
import org.scalastuff.scalaleafs.Alert

object Dialogs {

  def confirmation(text : String)(f : => JsCmd) = {
    JsCmd("alert('pos: ' + event.screenX + ', ' + event.screenY'")
  } 
}
