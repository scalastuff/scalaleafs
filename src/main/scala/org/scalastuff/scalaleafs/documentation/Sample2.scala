package net.scalaleafs.documentation

import net.scalaleafs._
import net.scalaleafs.implicits._

class Sample2 extends Template {
  val persons = Person("Joe", "Henri") :: Person("Michael", "Jackson") :: Nil
  var currentPerson = Person("Michael", "Jackson")
  val bind = ".person" #>   
    persons.map { person =>
        SetAttr("selected", "true", person == currentPerson) & 
        "#first-name" #> person.firstName &
        "#last-name" #> person.lastName 
    }
}