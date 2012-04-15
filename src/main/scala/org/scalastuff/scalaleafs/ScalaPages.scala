package org.scalastuff.scalaleafs

trait Config {
  val debugMode : Boolean = true
}

case class Configuration (
    debugMode : Boolean = false,
    contextPath : List[String] = Nil,
    jqueryUrl : String = "http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js"
)

