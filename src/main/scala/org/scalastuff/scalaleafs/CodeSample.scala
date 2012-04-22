package org.scalastuff.scalaleafs

import scala.xml.NodeSeq
import scala.xml.Text
import implicits._

trait CodeSample extends HasXmlTransformation {

  val transformation = "code-sample" #> ReplaceElem("table.code-sample tr") {
    "source[language=html]" #> ReplaceElem("td") {
      MkElem("pre")
    }
  }
  
}