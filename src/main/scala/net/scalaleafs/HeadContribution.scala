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

import scala.collection.mutable
import net.scalaleafs.implicits._
import scala.xml.NodeSeq
import scala.xml.Elem
import scala.xml.Node
import scala.xml.PCData

/**
 * Transformation that processes the <head> section of an HTML page. Resource links are resolved, contributions needed by the 
 * 
 * HeadContributions expects the whole page (HTML tag) as input.
 * Usually, an application's main frame class extends this trait.  
 */
trait HeadContributions extends XmlTransformation {
  abstract override def apply(xml : NodeSeq) : NodeSeq = {
   HeadContributions.render(R, super.apply(xml)) 
  }
}

object HeadContributions {
  
  def render(request : Request, xml : NodeSeq) : NodeSeq = {

    def processHead(html : Elem) : Elem = {
  
      def replaceResource : Node => Node = { node => node match {
        case elem : Elem =>
          elem.label match {
            case "link" =>
              val href = XmlHelpers.attr(elem, "href")
              if (!href.startsWith("http:")) {
                XmlHelpers.setAttr(elem, "href", request.resourceBaseUrl.resolve(request.server.resources.hashedResourcePathFor(href)).toLocalString)
              }
              else elem
            case "script" =>
              val src = XmlHelpers.attr(elem, "src")
              if (!src.startsWith("http:")) {
                XmlHelpers.setAttr(elem, "src", request.resourceBaseUrl.resolve(request.server.resources.hashedResourcePathFor(src)).toLocalString)
              }
              else elem
            case _ => elem
          }
        case node => node
      }}
        
      def addHeadContributions(head : Elem) = 
        head.copy(child = head.child.map(replaceResource) ++ request.headContributions.flatMap(_.render(request)))
  
      
// TODO since it scans head now, process it even without head contributions...
//      if (contributions.isEmpty) html
//      else 
      html.child.find(_.label == "head") match {
        case Some(head : Elem) => 
          html.copy(child = html.child.map { 
            case h : Elem if h eq head => addHeadContributions(head) 
            case h => h
          })
        case None => 
          html.copy(child = addHeadContributions(<head/>) ++ html.child)
      }
    }
    
    def additionalBodyContent : NodeSeq = { 
      val debugDiv = if (!request.debugMode) NodeSeq.Empty 
      else <div style="position: absolute; right: 10px; bottom: 10px; border: 1px solid red; color: red; padding: 4px">DEBUG MODE</div>
      request.session.mkPostRequestJsString(request.postRequestJs.toSeq) match {
        case "" => 
          debugDiv
        case cmd =>
          debugDiv ++
          <script type="text/javascript">
            {CommentedPCData("\n  leafs.onPageLoad(function() {\n    " + 
               cmd + 
            "\n  });\n")} 
          </script>
      }
    }
  
    def processBody(html : Elem) : Elem = html.child.find(_.label == "body") match {
      case Some(body : Elem) =>
        html.copy(child = html.child.map {
          case child if child eq body =>
            body.copy(child = body.child ++ additionalBodyContent)
          case child => child
        })
      case _ => 
        val (head, other) = html.child.partition(_.label == "head")
        html.copy(child = head ++ <body>{other ++ additionalBodyContent}</body>)
    }

    xml match {
      case elem : Elem =>
        if (elem.label == "html") {
          processBody(processHead(elem))
        } else elem
      case Seq(elem : Elem) => render(request, elem)
      case _ => xml
    }
  }
}

abstract class HeadContribution(val key : String) {
  def dependsOn : List[HeadContribution] = Nil
  def render(request : Request) : NodeSeq
}

class JavaScript(key : String, uri : String) extends HeadContribution(key) {
  def render(request : Request) = {
    <script type="text/javascript" src={uri} />
  }
}

class JavaScriptResource(c : Class[_], resource : String) extends HeadContribution(c.getName + "/" + resource) {
  def render(request : Request) = {
    var name = request.server.resources.hashedResourcePathFor(c, resource)
    <script type="text/javascript" src={request.resourceBaseUrl.resolve(name).toLocalString} />
  }
}

/**
 * Use JQuery as default javascript library.
 */

object LeafsJavaScriptResource extends JavaScriptResource(classOf[JavaScript], "leafs.js")

object JQueryUrl extends ConfigVar[String]("http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js")

object JQuery extends HeadContribution("jquery") {
  def render(request : Request) = {
    <script type="text/javascript" src={request.configuration(JQueryUrl)} />
  }
}
//
//object Callback extends HeadContribution("callback") {
//  override def dependsOn = JQuery :: LeafsJavaScriptResource :: Nil
//  def render(request : TransientRequest) = NodeSeq.Empty
//}
//
//object OnPopState extends HeadContribution("onPopState") {
//  override def dependsOn = Callback :: LeafsJavaScriptResource :: Nil
//  def render(request : TransientRequest) = {
//    val callbackId = R.callback( _.get("url") match {
//      case Some(location :: Nil) => 
//        if (location.startsWith("pop:")) R.popUrl(location.substring(4))
//        else R.url = location
//      case _ => 
//    }, "url" -> JsExp("url"))
//    <script type="text/javascript">      
//      function setUrl(url) {"{"}
        //callback('cb37463?url=' + url);
//        {callbackId}
//      {"}"};
//    </script>
//  }
//}

