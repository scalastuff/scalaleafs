package net 

import scala.concurrent.Future
package scalaleafs {
  import scala.xml.NodeSeq
  import scala.xml.Text
  import scala.collection.mutable.ArrayBuffer

  trait Implicits { 
    implicit def unparsedCssSelector(s : String) = new UnparsedCssSelector(s)
    implicit def stringToRenderNode(s : => String) = Xml.replaceWithString(s)
    implicit def xmlToRenderNode(xml : => NodeSeq) = Xml.replaceWith(xml)
    implicit def toJSCmdFun(f : Context => JSCmd) : JSCmdFun = new JSCmdFun(f)
    implicit def toExecutionContext(implicit context : Context) = context.executionContext
    implicit def toConfiguration(site : Site) = site.configuration
//    implicit def toConfiguration(site : Site) = site.configuration
    implicit class RichAny(a : Any) {
      def & (jsCmd : JSCmd) = jsCmd
    }
    implicit class RichUrlVal[A <% Val[Url]](v : A) {
      def head = v.mapVar(_.head)
      def headOption = v.mapVar(_.headOption)
      def tail  = v.mapVar(_.tail)
    }
  }
  
  package object implicits extends Implicits
}
package object scalaleafs extends Implicits 
