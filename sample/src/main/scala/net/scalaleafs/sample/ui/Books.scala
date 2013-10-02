package net.scalaleafs.sample.ui

import net.scalaleafs._

class Books(val tail : Var[UrlTail]) extends Template {

  def bind = xml => <h1>IN REPO</h1>
}