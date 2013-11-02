package net 

import scala.concurrent.Future
package scalaleafs2 {
  import scala.xml.NodeSeq
  import scala.xml.Text
  import scala.collection.mutable.ArrayBuffer

  trait Implicits { 
    implicit def unparsedCssSelector(s : String) = new UnparsedCssSelector(s)
    implicit def stringToRenderNode(s : => String) = Xml.replaceWithString(s)
    implicit def xmlToRenderNode(xml : => NodeSeq) = Xml.replaceWith(xml)
    implicit def toContextFun[A](f : => A) : Context => A = _ => f
    implicit def toJSCmdFun(f : Context => JSCmd) : JSCmdFun = new JSCmdFun(f)
    implicit def toExecutionContext(implicit context : Context) = context.executionContext
  }
  
  package object implicits extends Implicits
}
package object scalaleafs2 extends Implicits 
