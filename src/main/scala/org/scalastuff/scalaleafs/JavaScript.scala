package org.scalastuff.scalaleafs

import scala.xml.NodeSeq
import scala.xml.Text
import java.net.URI
import scala.xml.Utility
import java.util.concurrent.ConcurrentHashMap
import scala.io.Source

/**
 ** Generic java-script functionality. 
 **/

object JsExp {
  def apply(exp : String) = new JsRaw(exp)
  implicit def toJsCmd(exp : JsExp) = new JsCmd() {
    override def toString = exp.toString + ";"
  }
}

trait JsExp {
  def toCmd = toString match {
    case "" => Noop
    case s => new JsRawCmd(s + ";")
  }
}

object JsConst {
  def apply(value : String) = new JsRaw("'" + value + "'")
  def apply(value : Any) = new JsRaw(value.toString)
}

object JsCmd {
  def apply(cmd : String) = new JsRawCmd(cmd)
  implicit def toNoop(unit : Unit) : JsCmd = Noop
  implicit def toText(cmd : JsCmd) = Text(cmd.toString)
}

trait JsCmd {
  def & (cmd : JsCmd) : JsCmd = new JsCmdSeq(toSeq ++ cmd.toSeq)
  def toSeq = Seq(this)
}

protected class JsCmdSeq(seq : Seq[JsCmd]) extends JsCmd {
  override def toSeq = seq
  override def toString = toSeq.mkString("")
}

protected class JsRaw(exp : String) extends JsExp {
  override def toString = exp
}

protected class JsRawCmd(exp : String) extends JsCmd {
  override def toString = exp
}

case class JsReturn(value : JsExp) extends JsRawCmd("return " + value + ";")
object JsReturnFalse extends JsReturn(JsConst(false))
object JsReturnTrue extends JsReturn(JsConst(true))

case object Noop extends JsRawCmd("") {
  override def & (cmd : JsCmd) : JsCmd = cmd
  override def toSeq = Seq.empty
}

case class ReplaceHtml(id : String, xml : NodeSeq) extends JsRawCmd("leafs.replaceHtml('" + id + "', \"" + XmlHelpers.escape(xml.toString) + "\");")
case class RemoveNextSiblings(id : String, idBase : String) extends JsRawCmd("leafs.removeNextSiblings(\"" + id + "\", \"" + idBase + "\");")
case class Alert(message : String) extends JsRawCmd("alert('" + message + "');")
case class SetWindowLocation(uri : Any) extends JsRawCmd("window.location = '" + uri + "';")
case class ConsoleLog(msg : String) extends JsRawCmd("console.log('" + XmlHelpers.escape(msg) + "');")
case class PushState(uri : String, callback : String ) extends JsRawCmd("window.history.pushState(\"" + callback + "\", '" + "title" + "', '" + uri + "');")

