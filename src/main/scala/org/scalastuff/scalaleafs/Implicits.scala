package org.scalastuff.scalaleafs

import unfiltered.jetty.Http
import unfiltered.response.ResponseString
import scala.xml.Elem

package object implicits {
  implicit def withleafs(http : Http) = new WithLeafsUnfilteredHttp(http)
  implicit def toUnfilteredResponse(template : Template) = ResponseString(template.render.toString)
  implicit def toUnparsedCssSelector(s : String) = new UnparsedCssSelector(s)
  implicit def toRichElem(elem : Elem) = new RichElem(elem)
}
