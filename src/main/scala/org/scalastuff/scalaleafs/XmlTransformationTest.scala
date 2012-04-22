package org.scalastuff.scalaleafs
import org.junit.Test
import org.scalastuff.scalaleafs.implicits._
import org.junit.Assert

class XmlTransformationTest {

  @Test
  def testModifiers() {
    Assert.assertEquals(<div class="selected"/>, (SetClass("selected") & Ident)(<div/>))
    Assert.assertEquals(<div class="selected"/>, (Ident & SetClass("selected"))(<div/>))
    Assert.assertEquals(<div id="my-div" class="selected"/>, (SetClass("selected") & SetId("my-div"))(<div/>))
    Assert.assertEquals(<div id="my-div"/>, (SetClass("selected", false) & SetId("my-div"))(<div/>))
    Assert.assertEquals(<div id="my-div"/>, SetId("my-div")(<div/>))
    Assert.assertEquals(<div class="selected"/>, SetClass("selected")(<div/>))
  }

}