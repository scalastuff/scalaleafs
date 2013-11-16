package net.scalaleafs.sample

import net.scalaleafs.Template
import net.scalaleafs.Ident
import net.scalaleafs.JSCmd

class Schedule extends Template {
  def render = js(JSCmd("initCal()"))
}