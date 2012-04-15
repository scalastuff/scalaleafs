package org.scalastuff.scalaleafs

import scala.xml.NodeSeq
import java.net.URI
import scala.xml.Utility
import java.util.concurrent.ConcurrentHashMap
import scala.io.Source

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

object JsCmd {
  def apply(cmd : String) = new JsRawCmd(cmd)
  implicit def toNoop(unit : Unit) : JsCmd = Noop
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

case object Noop extends JsRawCmd("") {
  override def & (cmd : JsCmd) : JsCmd = cmd
  override def toSeq = Seq.empty
}

case class SetHtml(id : String, xml : NodeSeq) extends JsRawCmd("$('#" + id + "').html(\"" + XmlHelpers.escape(xml.toString) + "\");")
case class ReplaceHtml(id : String, xml : NodeSeq) extends JsRawCmd("$('#" + id + "').replaceWith(\"" + XmlHelpers.escape(xml.toString) + "\");")
case class Alert(message : String) extends JsRawCmd("alert('" + message + "');")
case class SetWindowLocation(uri : Any) extends JsRawCmd("window.location = '" + uri + "';")
case class ConsoleLog(msg : String) extends JsRawCmd("console.log('" + XmlHelpers.escape(msg) + "');")
case class PushState(uri : String, callback : String ) extends JsRawCmd("window.history.pushState(\"" + callback + "\", '" + "title" + "', '" + uri + "');")
