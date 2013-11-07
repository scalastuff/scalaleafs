package net.scalaleafs

import org.junit.Test
import net.scalaleafs.implicits._
import net.scalaleafs.Xml._
import org.junit.Assert

class XmlTransformationTest {

  @Test
  def testModifiers() {
    Assert.assertEquals(<div class="selected"/>, (setClass("selected") & Ident)(<div/>))
    Assert.assertEquals(<div class="selected"/>, (Ident & setClass("selected"))(<div/>))
    Assert.assertEquals(<div id="my-div" class="selected"/>, (setClass("selected") & setId("my-div"))(<div/>))
    Assert.assertEquals(<div id="my-div"/>, (setClass("selected").when(false) & setId("my-div"))(<div/>))
    Assert.assertEquals(<div id="my-div"/>, setId("my-div")(<div/>))
    Assert.assertEquals(<div class="selected"/>, setClass("selected")(<div/>))
  }

}