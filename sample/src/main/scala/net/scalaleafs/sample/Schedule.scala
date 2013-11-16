package net.scalaleafs.sample

import net.scalaleafs.Template
import net.scalaleafs.Ident

class Schedule extends Template {
  def render = Ident & js("initCal")
}