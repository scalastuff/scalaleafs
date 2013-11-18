package net.scalaleafs.sample

import net.scalaleafs.Template
import net.scalaleafs.Ident
import net.scalaleafs.JSCmd
import net.scalaleafs.StylesheetRef
import net.scalaleafs.JavaScriptLibrary
import net.scalaleafs.CompoundHeadContribution
import net.scalaleafs.JavaScriptLibrary
import net.scalaleafs.JavaScriptResource

object Calendar2JavaScriptResource extends JavaScriptLibrary("jquery.calendar.js", "jquery.calendar.js") 

class Calendar2 extends Template {
  def render = contrib(Calendar2JavaScriptResource) & exec(JSCmd("initCal()"))
}