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
import scala.concurrent.Future
import net.scalaleafs.implicits._
import scala.xml.NodeSeq
import scala.xml.Elem
import scala.xml.Node
import scala.xml.PCData
import java.util.UUID

/**
 * Transformation that processes the <head> section of an HTML page. Resource links are resolved, contributions needed by the 
 * 
 * HeadContributions expects the whole page (HTML tag) as input.
 * Usually, an application's main frame class extends this trait.  
 */
trait HeadContributions { this : Context =>
  private[scalaleafs] var _headContributionKeys : Set[String] = Set.empty
  private[scalaleafs] var _headContributions : Seq[HeadContribution] = Seq.empty
  
  def headContributions = 
    _headContributions
    
  def addHeadContribution(contribution : HeadContribution) {
    val key = contribution.key
    if (!window._headContributionKeys.contains(key)) {
      if (!_headContributionKeys.contains(key)) {
        
        // Add key first to prevent cyclic behavior.
        _headContributionKeys += key
        
        // Add dependencies before actual contribution.
        contribution.dependsOn.foreach(dep => addHeadContribution(dep))
        
        // Now add actual contribution.
        _headContributions :+= contribution
      }
    }
  }
}

object HeadContributions {
  
  def render(context : Context, xml : NodeSeq) : NodeSeq = {

    def processHead(html : Elem) : Elem = {
  
      def replaceResource : Node => Node = { node => node match {
        case elem : Elem =>
          elem.label match {
            case "link" =>
              val href = XmlHelpers.attr(elem, "href")
              if (!href.startsWith("http:")) {
                XmlHelpers.setAttr(elem, "href", context.site.resourcePath.mkString("/", "/", "/" + context.site.resources.hashedResourcePathFor(href.stripPrefix("/"))))
              }
              else elem
            case "script" =>
              val src = XmlHelpers.attr(elem, "src")
              if (!src.startsWith("http:")) {
                XmlHelpers.setAttr(elem, "src", context.site.resourcePath.mkString("/", "/", "/" + context.site.resources.hashedResourcePathFor(src.stripPrefix("/"))))
              }
              else elem
            case _ => elem
          }
        case node => node
      }}
        
      def addHeadContributions(head : Elem) = 
        head.copy(child = head.child.map(replaceResource) ++ context.headContributions.flatMap(_.render(context)))
  
      
// TODO since it scans head now, process it even without head contributions...
//      if (contributions.isEmpty) html
//      else 
      html.child.find(_.label == "head") match {
        case Some(head : Elem) => 
          html.copy(child = html.child.map { 
            case h : Elem if h eq head => addHeadContributions(head) 
            case h => h
          })
        case _ => 
          html.copy(child = addHeadContributions(<head/>) ++ html.child)
      }
    }
    
    def additionalBodyContent : NodeSeq = { 
      val debugDiv = 
        if (!context.debugMode) NodeSeq.Empty 
        else <div style="position: absolute; right: 10px; bottom: 10px; border: 1px solid red; color: red; padding: 4px">DEBUG MODE</div>
      context.site.mkPostRequestJsString(context._postRequestJs.toSeq) match {
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
      case Seq(elem : Elem) => 
        if (elem.label == "html") {
          processBody(processHead(elem))
        } else elem
      case _ => xml
    }
  }
}

abstract class HeadContribution {
  def key : String
  def dependsOn : List[HeadContribution] = Nil
  def render(context : Context) : NodeSeq
  def renderAdditional(context : Context) : JSCmd = Noop
  def & (contrib : HeadContribution) = new CompoundHeadContribution(this :: contrib :: Nil)
}

class CompoundHeadContribution(children: List[HeadContribution]) extends HeadContribution {
  lazy val key = UUID.randomUUID.toString
  def this(children : HeadContribution*) = this(children.toList)
  override def dependsOn = children
  def render(context : Context) = NodeSeq.Empty
  override def & (contrib : HeadContribution) = new CompoundHeadContribution(children ++ List(contrib))
}

class StylesheetRef(val key : String, uri : String, media : String = "") extends HeadContribution {
  def render(context : Context) = {
    if (media.isEmpty()) <link rel="stylesheet" href={uri} />
    else <link rel="stylesheet" href={uri} media={media} />
  }
}

class StylesheetResource(c : Class[_], resource : String, media : String = "") extends HeadContribution {
  val key = c.getName + "/" + resource
  def render(context : Context) : NodeSeq = {
    var name = context.site.resources.hashedResourcePathFor(c, resource)
    val path = context.site.resourcePath.mkString("/", "/", "/" + name)
    if (media.isEmpty()) <link rel="stylesheet" href={path} />
    else <link rel="stylesheet" href={path} media={media} />
  }
}

class JavaScriptLibrary(val key : String, uri : String) extends HeadContribution {
  def render(context : Context) = {
    <script type="text/javascript" src={uri} ></script>
  }
  override def renderAdditional(context : Context) = {
    LoadJavaScript(uri)
  }
}

class JavaScriptResource(c : Class[_], resource : String) extends HeadContribution {
  val key = c.getName + "/" + resource
  def render(context : Context) : NodeSeq = {
    var name = context.site.resources.hashedResourcePathFor(c, resource)
    <script type="text/javascript" src={context.site.resourcePath.mkString("/", "/", "/" + name)}></script>
  }
  override def renderAdditional(context : Context) = {
    var name = context.site.resources.hashedResourcePathFor(c, resource)
    LoadJavaScript(context.site.resourcePath.mkString("/", "/", "/" + name))
  }
}

/**
 * Use JQuery as default JavaScript library.
 */

object LeafsJavaScriptResource extends JavaScriptResource(classOf[JavaScriptLibrary], "leafs.js") {
  override def render(context : Context) = {
    super.render(context) ++ 
    <script type="text/javascript">
      window.id = '{context.window.id}'; 
      leafs.onPageUnload('{context.callbackId(context => _ => println("DELETE WINDOW"))}');
    </script>
  }
}

object JQueryUrl extends ConfigVal[String]("http://code.jquery.com/jquery-1.10.2.min.js")

object JQuery extends HeadContribution {
  def key = "JQuery"
  def render(context : Context) = {
    <script type="text/javascript" src={JQueryUrl(context)}></script>
  }
}

object OnPopStateHeadContribution extends HeadContribution {
  def key = "OnPopState"
  override def dependsOn = LeafsJavaScriptResource :: Nil
  def render(context : Context) = {
    <script type="text/javascript"> 
      window.setLocationCallback = '{ context.callbackId { context => map => 
        Future.successful {
          map.find(_._1 == "value").map(_._2)  match {
            case Some(url) =>
              if (url.startsWith("pop:")) context.popUrl(url.substring(4))
              else context.url = url
            case None =>
          }       
        }
      }}'
    </script>
  }  
} 
