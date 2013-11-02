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
package net.scalaleafs2

import scala.xml.Elem
import scala.xml.NodeSeq
import implicits._
import scala.concurrent.Future

object Html extends Html

class OperationMagnet[A](val magnetFunction : Context => Future[A]) extends AnyVal {
  @inline
  def apply(context : Context) = magnetFunction(context)
}

object OperationMagnet {
  implicit def magnet1[A](f : Context => Future[A]) = new OperationMagnet[A](f)
  implicit def magnet2[A](f : Context => A) = new OperationMagnet[A](context => Future.successful(f(context)))
  implicit def magnet3[A](f : => Future[A]) = new OperationMagnet[A](context => f)
  implicit def magnet4[A](f : => A) = new OperationMagnet[A](context => Future.successful(f))
  
  implicit def JSNoopMagnet1(f : Context => Future[Unit]) = new OperationMagnet[JSCmd](context => f(context).map(_ => JSNoop)(context.executionContext))
  implicit def JSNoopMagnet2(f : Context => Unit) = new OperationMagnet[JSCmd](context => { f(context); Future.successful(JSNoop) })
  implicit def JSNoopMagnet3(f : => Future[Unit]) = new OperationMagnet[JSCmd](context => f.map(_ => JSNoop)(context.executionContext))
  implicit def JSNoopMagnet4(f : => Unit) = new OperationMagnet[JSCmd](context => { f; Future.successful(JSNoop) })
}

//class SyncOperationMagnet[A](val magnetFunction : Context => A) extends Function[Context, A] {
//  @inline
//  def apply(context : Context) = magnetFunction(context)
//}
//
//object SyncOperationMagnet {
//  implicit def magnet2[A](f : Context => A) = new SyncOperationMagnet[A](context => f(context))
////  implicit def magnet4[A](f : => A) = new SyncOperationMagnet[A](context => f)
//  
////  implicit def JSNoopMagnet2(f : Context => Unit) = new SyncOperationMagnet[JSCmd](context => { f(context); JSNoop })
////  implicit def JSNoopMagnet4(f : => Unit) = new SyncOperationMagnet[JSCmd](context => { f; JSNoop })
//}

trait Html {
 
//  def callback(f : => Unit) : Context => JSCmd = _.callback(Future.successful(f))

//  def onclick(f : => JSCmd) : ElemModifier = {
//    Xml.setAttr("onclick", context => context.callback(_ => Future.successful(context.addPostRequestJs(f))) & JsReturnFalse)
//  }
//  
  def onclick(f : OperationMagnet[JSCmd]) : ElemModifier = {
    Xml.setAttr("onclick", { context =>
      import context.executionContext
      context.callback(_ => f(context).map(context.addPostRequestJs(_))) & JsReturnFalse
      })
  }
  
//  def onchange(f : String => JSCmd) : ElemModifier = {
//    Xml.setAttr("onchange", R.callback(JSExp("this.value"))(s => f(s)))
//  }
//  
//  def select[A](values : Seq[A])(f : A => JSCmd) = {
//    Xml.setAttr("onchange", R.callback(JSExp("this.selectedIndex"))(s => f(values(Integer.parseInt(s))))) & 
//    Xml.setContent(content_ => values.map(v => <option>{v}</option>))
//  }
//  
//  def linkup = new ElemTransformation { 
//    def apply(elem : Elem) = {
//      XmlHelpers.attr(elem, "href") match {
//        case "" => elem
//        case href =>
//          //XmlHelpers.setAttr(XmlHelpers.setAttr(elem, href, "CTX/" + href), "onclick", R.callback(_ => R.changeUrl(href)).toString)
//          XmlHelpers.setAttr(elem, href, "CTX/" + href)
//      }          
//    }
//  }
}