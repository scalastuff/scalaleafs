package org.scalastuff.scalaleafs

import java.net.URI
import scala.collection.mutable
import scala.collection.generic.Growable
import scala.collection.immutable.TreeMap
import scala.xml.NodeSeq

case class UrlContext(protocol : String, host : String, port : String, path : Seq[String]) {
  def resolve(path : String) : Url = 
    Url.parse(path) match {
      case (path, parameters) => Url(this, path, parameters)
    }
}


object Url {
//  def apply(path : Seq[String]) = R.url.copy(remainingPath = path.filter(_ != ""))
  
  /**
   * parse a path relative to the  
   */
  def parse(path : String) : (Seq[String], Map[String, Seq[String]]) = {
    val index = path.indexOf('?')
    if (index >= 0) {
      val remainingPath = parsePath(path.substring(0, index), '/') 
      val parameters = parsePath(path.substring(index + 1), '&')
      val map = new mutable.ListMap[String, Seq[String]] 
      parameters.map(parsePath(_, '=')).foreach { s =>
        if (!s.isEmpty) {
          val (key, value) = if (s.size == 1) (s(0), "") else (s(0), s(1))
          val seq = map.get(key) match {
            case Some(seq) => seq ++ Seq(value)
            case None => Seq(value)
          }
          map += (key -> seq)
        }
        
      }
      (remainingPath, map.toMap)
    }
    else {
      (parsePath(path, '/'), Map.empty)
    }
  }
  
//  def parsePath(path : String) : Seq[String] =
//    parsePath(path, '/')
  
  def parsePath(path : String, sep : Char) : Seq[String] = {
    val builder = Seq.newBuilder[String]
    var lastIndex = 0
    var index = 0;
    while (index >= 0) {
      index = path.indexOf(sep, lastIndex)
      if (index > lastIndex) {
        builder.+=(path.substring(lastIndex, index))
      } else if (index < 0 && lastIndex < path.size) {
        builder += path.substring(lastIndex)
      }
      lastIndex = index + 1
    }
    builder.result
  }
}

object UrlTrail {
  
  /**
   * Creates a trail to a url denoted by path and parameters, starting at the context. 
   */
  def apply(context : UrlContext, path : List[String], parameters : Map[String, Seq[String]]) : UrlTrail =
    UrlTrail(Url(context, Nil, parameters), path)
}

/**
 * A trail is a path to a url. Starting at a context, one can advance the trail to the end, where the
 * trail reached is url. Typically, advancing a trail to its end corresponds with the way pages are rendered.
 */
case class UrlTrail(current : Url, remainder : List[String]) {

  /**
   * Trail's target url. Typically contains the full request url. 
   */
  lazy val url = 
    Url(current.context, current.path ++ remainder, current.parameters)
    
  /**
   * Is the trail at its start position?
   */
  def atStart = 
    current == Nil
    
  /**
   * Has the trail reached its destination?
   */
  def atEnd = 
    remainder == Nil
  
  /**
   * Move forward on the trail. Should only be called when not at end.
   */
  def advance = 
    UrlTrail(current.child(remainder.head), remainder.tail)

  /**
   * Go back one step on the trail. Should only be called when not at start.
   */
  // TODO better name
  def goback =
    UrlTrail(current.parent, current.path.last :: remainder)
}

case class Url(context : UrlContext, path : Seq[String], parameters : Map[String, Seq[String]]) {

  // TODO add context path and querystring
//  private lazy val localUri = new URI("/" + currentPath.mkString("/") + remainingPath.mkString("/"))
//  
//  lazy val uri = new URI(context + currentPath.mkString("/") + remainingPath.mkString("/") + queryString)
  
//  def unadvance = 
//    Url(context, currentPath.dropRight(1), currentPath.last +: remainingPath, parameters)
    
  def parent =
    Url(context, path.dropRight(1), parameters)
    
  def child(segment : String) = 
    Url(context, path :+ segment, parameters)
    
  def resolve(path : String) : Url = 
    Url.parse(path) match {
      case (p, pars) => Url(context, this.path ++ p, pars)
    }
//
//  def resolve(path : Seq[String]) : Url = 
//    Url(context, currentPath, remainingPath ++ path, Map.empty)
  
//  def resolve(uri : URI) : Url = fromUri(this.uri.resolve(uri))
//  
//  def hasSameContext(uri : URI) = 
//    uri.getScheme == context.getScheme && 
//    uri.getAuthority == contextUri.getAuthority && 
//    uri.getPath.startsWith(contextUri.getPath)
//        

//  private def queryString = if (parameters.isEmpty) "" else "?" + (for ((k, v) <- parameters) yield k + "=" + v).mkString("&")
//
//  private def fromUri(uri : URI) =
//    if (hasSameContext(uri)) new Url(context, Nil, uri.getPath.substring(context.getPath.length).split("/").filter(_ != "").toList, Map.empty)
//    else new Url(uri, Nil, Nil, Map.empty)
//  
//  private def parseQuery(query : String) {
//      query.split("&").toSeq.map(_.split("="))
//    }
  
  override def toString = 
    toString(true, true)
    
  def toLocalString = 
    toString(false, true)

  def toString(printServer : Boolean, printContext : Boolean) : String = 
    toString(printServer, printContext, new StringBuilder).toString

  def toString(printServer : Boolean, printContext : Boolean, builder : StringBuilder) = {
    if (!context.host.isEmpty && printServer) {
      if (context.protocol.isEmpty()) 
        builder.append("http)")
      else 
        builder.append(context.protocol)
      builder.append("://")
      builder.append(context.host)
      if (!context.port.isEmpty) {
        builder.append(":").append(context.port)
      }
    }
    builder.append('/')
    if (printContext) {
      for (p <- context.path) {
        builder.append(p).append('/')
      }
    }
    var sep = ""
    for (p <- path) {
      builder.append(sep).append(p)
      sep = "/"
    }
    sep = "?"
    for ((name, values) <- parameters; value <- values) {
      builder.append(sep).append(name).append("=").append(value)
      sep = "&"
    }
    builder
  }
}

/**
 * Implement this trait to be able to react on url changes. Handlers are called when a url is changed in 
 * an ajax callback. It allows for url changes without page switches. 
 */
trait UrlHandler {
  def trail : Var[UrlTrail]
  def handleUrl(trail : UrlTrail) {
    this.trail.set(trail)
  }
  R.addUrlHandler(this)
}

trait UrlManager {
  private var urlHandlers : mutable.Map[(UrlContext, Seq[String]), UrlHandler] = null
  
  private[scalaleafs] def addUrlHandler(handler : UrlHandler) {
    if (urlHandlers == null) {
      urlHandlers = new mutable.HashMap[(UrlContext, Seq[String]), UrlHandler]()
    }
    val trail = handler.trail.get
    urlHandlers.put((trail.current.context, trail.current.path), handler)
  }
  
  def handleUrl(trail : UrlTrail) : JsCmd = {
    var result : Option[JsCmd] = None 
    if (urlHandlers != null) {
      var currentPath = trail.remainder
      var remainingPath : Seq[String] = Nil
      var stop = false;
      do {
        if (currentPath.isEmpty) stop = true;
        handleUrl(trail.url, trail.remainder, trail.url.parameters) match {
          case Some(cmd) => 
            result = Some(cmd)
          case None => 
            if (currentPath.nonEmpty) {
              remainingPath = currentPath.last +: remainingPath
              currentPath = currentPath.dropRight(1)
            }
        }
        
      } while (result == None && !stop)
    }
    result match {
      case Some(cmd) => cmd
      case None => Noop//SetWindowLocation(url)
    }
  }
  
  private def handleUrl(currentUrl : Url, remainingPath : Seq[String], parameters : Map[String, Seq[String]]) : Option[JsCmd] = {
    urlHandlers.get((currentUrl.context, currentUrl.path)) match {
      case Some(handler) => 
        val url = currentUrl.copy(path = currentUrl.path ++ remainingPath)
        // Only need to handle if url is, in fact, different.
        if (url != handler.trail.get.url) {
          // Try handle the url.
          handler.handleUrl(url)
          // Url was handled?
          if (url == handler.url.get) {
            Some(JsCmd("window.history.pushState(\"" + url + "\", '" + "title" + "', '" + url + "');"))
          }
          // Url not handled, try next handler.
          else None
        }
        // Nothing to do if url hasn't changed
        else Some(Noop) 
        // No handler found, try next handler.
      case None => None
      }
  }
}

trait Handler {
  def trail : UrlTrail
  
  lazy val remainingPath = Var(trail.remainder)
  def renderParameters(pf : PartialFunction[(String, List[String]), NodeSeq => NodeSeq]) = null
}

class MyPage extends Handler {
  
  def render = renderParameters {
    case ("page", rest) => SetAttr("asfd", "df")
  }
}
