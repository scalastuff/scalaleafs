package org.scalastuff.scalaleafs

import unfiltered.jetty.Http
import unfiltered.response.ResponseString
import scala.xml.Elem
import scala.xml.NodeSeq

package object implicits {
  implicit def withleafs(http : Http) = 
    new WithLeafsUnfilteredHttp(http)
  
  implicit def toUnfilteredResponse(template : Template) = 
    ResponseString(template.render.toString)
  
  implicit def toUnparsedCssSelector(s : String) = 
    new UnparsedCssSelector(s)
  
  implicit def toCssSelector(s : String) = 
    CssSelector.getOrParse(s)

  implicit def toRichElem(elem : Elem) = new RichElem(elem)
  
  implicit def seqXmlTransformation(transformations : Seq[NodeSeq => NodeSeq]) = 
    new SeqXmlTransformation(transformations)
  
  implicit def toCompoundChangeable2[A, B](changeables : (Changeable[A], Changeable[B])) = 
    new CompoundChangeable2[A, B](changeables._1, changeables._2)

  implicit def toCompoundChangeable3[A, B, C](changeables : (Changeable[A], Changeable[B], Changeable[C])) = 
    new CompoundChangeable3[A, B, C](changeables._1, changeables._2, changeables._3)

  implicit def toCompoundChangeable4[A, B, C, D](changeables : (Changeable[A], Changeable[B], Changeable[C], Changeable[D])) = 
    new CompoundChangeable4[A, B, C, D](changeables._1, changeables._2, changeables._3, changeables._4)

  implicit def toOptionVar[A](initialValue : Option[A]) = new Object {
    def toVar = Var(initialValue)
  }
    
  implicit def toIterableVar[A, B <: Iterable[A]](initialValue : B) = new Object {
    def toVar = Var(initialValue)
  }
  
  implicit def toVar[A](initialValue : A) = new Object {
    def toVar = Var(initialValue)
  }
}
