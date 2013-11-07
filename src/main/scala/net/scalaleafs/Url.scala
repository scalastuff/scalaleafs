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
  override def toString = protocol + "://" + host + ":" + port + "/" + path.mkString("/") + "/"
}

object Url {
  
  /**
   * parse a path relative to the context.
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

case class Url(context : UrlContext, path : List[String], parameters : Map[String, Seq[String]]) {

  def head = path.head
  def headOption = path.headOption
  def tail = path.tail
  
  def parent =
    Url(context, path.dropRight(1), parameters)
    
  def child(segment : String) = 
    Url(context, path :+ segment, parameters)
    
  def resolve(path : String) : Url = {
    // Is it an absolute path?
    path.indexOf("://") match {
      // Relative path.
      case -1 => 
        val (p, pars) = Url.parse(path)
        // Paths starting with / are relative to context.
        if (path.trim().startsWith("/"))
          Url(context, p, pars)
        // Normal resolve simply appends current path and new path.
        else 
          Url(context, this.path ++ p, pars)
      // Absolute path.
      case i0 => 
        val protocol = path.substring(0, i0).trim()
        val hostAndPortEndIndex = path.indexOf('/', i0 + 3) match {
          case -1 => path.length
          case i => i
        }
        val (host, port) = path.indexOf(':', i0 + 3) match {
          case i if i < 0 || i >= hostAndPortEndIndex => (path.substring(i0 + 3, hostAndPortEndIndex), "")
          case i => (path.substring(i0 + 3, i), path.substring(i + 1, hostAndPortEndIndex))
        }
        val (p, pars) = Url.parse(path.substring(hostAndPortEndIndex))
        // Path is relative to current context?
        if (context.protocol == protocol && context.host == host && context.port == port && p.startsWith(context.path)) 
          Url(context, p.drop(context.path.size), pars)
        // Or was it a path with a different context?
        else
          Url(UrlContext(protocol, host, port, Nil), p, pars)
    }
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
