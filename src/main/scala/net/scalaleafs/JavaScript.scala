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
package net.scalaleafs

import scala.xml.{Text, NodeSeq}

/**
 ** Generic java-script functionality. 
 **/

object JSExp {
  def apply(exp : String) = new JsRaw(exp)
  implicit def toJSCmd(exp : JSExp) = new JSCmd() {
    override def toString = exp.toString + ";"
  }
}

trait JSExp {
  def toCmd = toString match {
    case "" => Noop
    case s => new JSRawCmd(s + ";")
  }
}

object JsConst {
  def apply(value : String) = new JsRaw("'" + value + "'")
  def apply(value : Any) = new JsRaw(value.toString)
}

object JSCmd {
  def apply(cmd : String) = new JSRawCmd(cmd)
  implicit def toText(cmd : JSCmd) = Text(cmd.toString)
  implicit def toString(cmd : JSCmd) = cmd.toString
  implicit def fromBoolean(b: Boolean): JSCmd = if (b) JsConst(true) else JsConst(false)
  
}

trait JSCmd {
  def & (cmd : Noop) : JSCmd = this
  def & (cmd : JSCmd) : JSCmd = new JSCmdSeq(toSeq ++ cmd.toSeq)
  def toSeq = Seq(this)
}

class JSCmdFun(f : Context => JSCmd) extends Function[Context, JSCmd] { outer =>
  def apply(context : Context) = f(context)
  def &(other : Noop) = this
  def &(other : JSCmd) = new JSCmdFun(context => f(context) & other)
  def &(other : Context => JSCmd) = new JSCmdFun(context => f(context) & other(context))
}

object JSCmdFun {
  implicit def toString(f : JSCmdFun) : Context => String = context => f(context).toString
  implicit def toJSCmdFun(cmd : JSCmd) : JSCmdFun = new JSCmdFun(_ => cmd)
}


protected class JSCmdSeq(seq : Seq[JSCmd]) extends JSCmd {
  override def toSeq = seq
  override def toString = toSeq.mkString("")
}

protected class JsRaw(exp : String) extends JSExp {
  override def toString = exp
}

protected class JSRawCmd(exp : String) extends JSCmd {
  override def toString = exp
}

case class JsReturn(value : JSExp) extends JSRawCmd("return " + value + ";")
object JsReturnFalse extends JsReturn(JsConst(false))
object JsReturnTrue extends JsReturn(JsConst(true))


class Noop extends JSRawCmd("") {
  override def & (cmd : JSCmd) : JSCmd = cmd
  override def toSeq = Seq.empty
}

case object Noop extends Noop

case class ReplaceHtml(id : String, xml : NodeSeq) extends JSRawCmd("leafs.replaceHtml('" + id + "', \"" + XmlHelpers.escape(xml.toString) + "\");")
case class RemoveNextSiblings(id : String, idBase : String) extends JSRawCmd("leafs.removeNextSiblings(\"" + id + "\", \"" + idBase + "\");")
case class Alert(message : String) extends JSRawCmd("alert('" + message + "');")
case class SetWindowLocation(uri : Any) extends JSRawCmd("window.location = '" + uri + "';")
case class ConsoleLog(msg : String) extends JSRawCmd("console.log('" + XmlHelpers.escape(msg) + "');")
case class PushState(uri : String, callback : String ) extends JSRawCmd("window.history.pushState(\"" + callback + "\", '" + "title" + "', '" + uri + "');")
case class LoadJavaScript(uri : String) extends JSRawCmd("leafs.loadJavascript('" + uri + "');")
