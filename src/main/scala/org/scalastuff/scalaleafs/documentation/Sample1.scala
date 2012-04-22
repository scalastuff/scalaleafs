package org.scalastuff.scalaleafs.documentation

import org.scalastuff.scalaleafs.Template
import org.scalastuff.scalaleafs.implicits._

case class Person(firstName : String, lastName : String)

class Sample1 extends Template {
  val person = Person("Joe", "Henri")
  val bind = 
  "#first-name" #> person.firstName &
  "#last-name" #> person.lastName
}