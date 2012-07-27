/**
 * Copyright (c) 2012 Ruud Diterwich.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */

import scala.xml.NodeSeq
import scala.xml.Elem

/**
 * Allow 2 kinds of implicit imports:
 * import net.scalaleafs._  :  Imports everything, including implicits
 * import net.scalaleafs.implicits._  :  Imports just implicits
 */
package net {
  package scalaleafs {
    
    trait Implicits {
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
    
    package object implicits extends Implicits
  }
  package object scalaleafs extends Implicits with Xml
}