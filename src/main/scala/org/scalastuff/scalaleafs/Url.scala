package org.scalastuff.scalaleafs

import java.net.URI
import scala.collection.mutable

object Url {
  def apply(path : List[String]) = R.url.copy(remainingPath = path.filter(_ != ""))
}

case class Url(context : URI, currentPath : List[String], remainingPath : List[String], parameters : Map[String, List[String]]) {

  private lazy val localUri = new URI("/" + currentPath.mkString("/") + remainingPath.mkString("/"))
  
  lazy val uri = new URI(context + currentPath.mkString("/") + remainingPath.mkString("/") + queryString)
  
  def child = 
    Url(context, currentPath ++ List(remainingPath.head), remainingPath.tail, parameters)
  
  def parent = 
    Url(context, currentPath.dropRight(1), currentPath.last :: remainingPath, parameters)
    
  def resolve(uri : String) : Url = fromUri(this.uri.resolve(uri))
  
  def resolve(uri : URI) : Url = fromUri(this.uri.resolve(uri))
  
  def hasSameContext(uri : URI) = 
    uri.getScheme == context.getScheme && 
    uri.getAuthority == context.getAuthority && 
    uri.getPath.startsWith(context.getPath)
        
  override def toString = uri.toString

  private def queryString = if (parameters.isEmpty) "" else "?" + (for ((k, v) <- parameters) yield k + "=" + v).mkString("&")

  private def fromUri(uri : URI) =
    if (hasSameContext(uri)) new Url(context, Nil, uri.getPath.substring(context.getPath.length).split("/").filter(_ != "").toList, Map.empty)
    else new Url(uri, Nil, Nil, Map.empty)
  
  private def parseQuery(query : String) {
      query.split("&").toSeq.map(_.split("="))
    }
}

trait UrlHandler {
  def url : Var[Url]
  def handleUrl(url : Url) {
    this.url.set(url)
  }
  R.addUrlHandler(this)
}

trait UrlManager {
  private var urlHandlers : mutable.Map[(URI, List[String]), UrlHandler] = null
  
  private[scalaleafs] def addUrlHandler(handler : UrlHandler) {
    if (urlHandlers == null) {
      urlHandlers = new mutable.HashMap[(URI, List[String]), UrlHandler]()
    }
    val url = handler.url.get
    urlHandlers.put((url.context, url.currentPath), handler)
  }
  
  def handleUrl(url : Url) : JsCmd = {
    var result : Option[JsCmd] = None 
    if (urlHandlers != null) {
      var currentPath = url.remainingPath
      var remainingPath : List[String] = Nil
      var stop = false;
      do {
        if (currentPath.isEmpty) stop = true;
        handleUrl(url.context, currentPath, remainingPath, url.parameters) match {
          case Some(cmd) => 
            result = Some(cmd)
          case None => 
            if (currentPath.nonEmpty) {
              remainingPath = currentPath.last :: remainingPath
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
  
  private def handleUrl(context : URI, currentPath : List[String], remainingPath : List[String], parameters : Map[String, List[String]]) : Option[JsCmd] = {
    urlHandlers.get((context, currentPath)) match {
      case Some(handler) => 
        val url = Url(context, currentPath, remainingPath, parameters)
        // Only need to handle if url is, in fact, different.
        if (url != handler.url.get) {
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

