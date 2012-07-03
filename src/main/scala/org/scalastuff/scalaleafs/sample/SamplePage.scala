package org.scalastuff.scalaleafs.sample

import org.scalastuff.scalaleafs.implicits._
import org.scalastuff.scalaleafs.UrlHandler
import org.scalastuff.scalaleafs.Html
import org.scalastuff.scalaleafs.ReplaceHtml
import org.scalastuff.scalaleafs.Alert
import org.scalastuff.scalaleafs.RemoveClass
import org.scalastuff.scalaleafs.Url

object SamplePage {
}

class SamplePage(val url : Url) {

  val bind =  
    "#my-elem" #> (elem => <div id="bla"><span>Replacement text</span>{elem.child}</div>) &
    ".enabled" #> RemoveClass("enabled")
    "#my-button" #>  Html.onclick {
      println("CLICKED!")
      Alert("Clicked the button!")
      ReplaceHtml("my-button", <h1>Button replaced!!!</h1>)
    }
}