package net.scalaleafs

import net.scalaleafs.implicits._
import net.scalaleafs.Xml._
import org.scalatest.FlatSpec

class XmlTransformationTest extends FlatSpec {

  "Elem modifiers" should "work" in { 
    assert(<div class="selected"/> === (setClass("selected") & Ident)(<div/>))
    assert(<div class="selected"/> === (Ident & setClass("selected"))(<div/>))
    assert(<div id="my-div" class="selected"/> === (setClass("selected") & setId("my-div"))(<div/>))
    assert(<div id="my-div"/> === (setClass("selected").when(false) & setId("my-div"))(<div/>))
    assert(<div id="my-div"/> === setId("my-div")(<div/>))
    assert(<div class="selected"/> === setClass("selected")(<div/>))
  }

}