package net.scalaleafs.sample

import net.scalaleafs.Template
import net.scalaleafs.Ident
import net.scalaleafs.JSCmd
import net.scalaleafs.StylesheetRef
import net.scalaleafs.JavaScriptLibrary
import net.scalaleafs.CompoundHeadContribution
import net.scalaleafs.JavaScriptLibrary


class Schedule extends Template {
  def render = exec(JSCmd("initCal()"))
}