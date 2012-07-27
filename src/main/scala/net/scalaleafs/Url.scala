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

import java.net.URI
import scala.collection.mutable
import scala.collection.generic.Growable
import scala.collection.immutable.TreeMap
import scala.xml.NodeSeq

/**
 * The context of a url is the part that lies outside the scope of the web application.
 */
case class UrlContext(protocol : String, host : String, port : String, path : Seq[String]) {
  def resolve(path : String) : Url = 
    Url.parse(path) match {
      case (path, parameters) => Url(this, path, parameters)
    }
}

object Url {
  
  /**
   * parse a path relative to the  
   */
  def parse(path : String) : (List[String], Map[String, Seq[String]]) = {
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
  
  def parsePath(path : String) : List[String] =
    parsePath(path, '/')
  
  def parsePath(path : String, sep : Char) : List[String] = {
    val builder = List.newBuilder[String]
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
  
  def apply(url : Url) : UrlTrail = 
    UrlTrail(url, url.path.toList)
}

/**
 * A trail is a path to a url. Starting at a context, one can advance the trail to the end, where the
 * trail reached its url. Typically, advancing a trail to its end corresponds with the way pages are rendered.
 */
case class UrlTrail(url : Url, remainder : List[String]) {
  def current = 
    url.path.dropRight(remainder.size)
    
  def advance = 
    UrlTrail(url, remainder.tail)
}

case class Url(context : UrlContext, path : Seq[String], parameters : Map[String, Seq[String]]) {
    
  def parent =
    Url(context, path.dropRight(1), parameters)
    
  def child(segment : String) = 
    Url(context, path :+ segment, parameters)
    
  def resolve(path : String) : Url =
    Url.parse(path) match {
      case (p, pars) if p.startsWith("/") => 
        Url(context, p, pars)
      case (p, pars) =>
        Url(context, this.path ++ p, pars)
    }
  
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
  R.initialRequest.urlManager.addUrlHandler(this)
}

class UrlManager {
  private var urlHandlers : mutable.Map[(UrlContext, Seq[String]), UrlHandler] = null
  
  private[scalaleafs] def addUrlHandler(handler : UrlHandler) {
    if (urlHandlers == null) {
      urlHandlers = new mutable.HashMap[(UrlContext, Seq[String]), UrlHandler]()
    }
    val trail = handler.trail.get
    urlHandlers.put((trail.url.context, trail.current), handler)
  }
  
 def handleUrl(url : Url) : JSCmd = {
    var result : Option[JSCmd] = None 
    if (urlHandlers != null) {
      var currentPath = url.path
      var remainingPath : List[String] = Nil
      var stop = false;
      do {
        if (currentPath.isEmpty) stop = true;
        handleUrl(url.context, url, currentPath, remainingPath, url.parameters) match {
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
  
  private def handleUrl(context : UrlContext, url : Url, currentPath : Seq[String], remainingPath : List[String], parameters : Map[String, Seq[String]]) : Option[JSCmd] = {
    urlHandlers.get((context, currentPath)) match {
      case Some(handler) => 
        val trail = UrlTrail(url, remainingPath)
        // Only need to handle if url is, in fact, different.
        if (trail != handler.trail.get) {
          // Try handle the url.
          handler.handleUrl(trail)
          // Url was handled?
          if (trail == handler.trail.get) {
            Some(JSCmd("window.history.pushState(\"" + url + "\", '" + "title" + "', '" + url + "');"))
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
