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

import scala.collection.JavaConversions._
import scala.xml.NodeSeq

import grizzled.slf4j.Logging
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletConfig
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

trait LeafsServletProcessor extends Logging {
  
  private var server : Server = null;
  private var ajaxCallbackPrefix = "";
  private var ajaxFormPostPrefix = "";
  private var resourcePrefix = "";
  protected val configuration : Configuration
  protected def render(trail : UrlTrail) : NodeSeq

  def initialize() {
    server = new Server(getClass.getPackage, configuration)
    ajaxCallbackPrefix = server.ajaxCallbackPath.mkString("/", "/", "/")
    ajaxFormPostPrefix = server.ajaxFormPostPath.mkString("/", "/", "")
    resourcePrefix = server.resourcePath.mkString("/", "/", "/")
  }
  
  protected def process(request : HttpServletRequest, response : ServletResponse, chain : FilterChain) {
    try {
      val startTime = System.currentTimeMillis
      val session = getSession(request)
      val servletPath = request.getServletPath()
      val parameters : Map[String, Seq[String]] = request.getParameterMap().toMap.map(kv => kv._1.toString -> kv._2.asInstanceOf[Array[String]].toSeq)
      if (request.getMethod == "GET" && servletPath.startsWith(ajaxCallbackPrefix)) {
        val js = session.handleAjaxCallback(servletPath.substring(ajaxCallbackPrefix.length), parameters)
        response.getWriter.write(js)
        response.flushBuffer
      }
      else if (request.getMethod == "POST" && servletPath.startsWith(ajaxFormPostPrefix)) {
        val js = session.handleAjaxFormPost(parameters)
            response.getWriter.write(js)
            response.flushBuffer
      } 
      else if (request.getMethod == "GET" && servletPath.startsWith(resourcePrefix)) {
        session.handleResource(servletPath.substring(resourcePrefix.length)) match {
          case Some((bytes, resourceType)) =>
            response.setContentType(resourceType.contentType)
            response.getOutputStream.write(bytes)
            response.flushBuffer
          case None => 
            chain.doFilter(request, response)
        }
      } 
      else if (request.getMethod == "GET") {
        val urlContext = UrlContext(request.getScheme(), request.getServerName(), Integer.toString(request.getLocalPort), Url.parsePath(request.getContextPath))
        val path = 
          if (request.getPathInfo == null) Url.parsePath(request.getServletPath())
          else Url.parsePath(request.getServletPath()) ++ Url.parsePath(request.getPathInfo)
        val url = new Url(urlContext, path, parameters)
        session.handleRequest(url, { () =>
          val xml = render(UrlTrail(url, path))
          val outputString = xml.toString
          response.setContentType("text/html")
          response.getWriter.append(outputString);
          response.flushBuffer;
        })
      }
      else {
        chain.doFilter(request, response)
      }
      println("Processed " + request.getRequestURI() + " (" + (System.currentTimeMillis- startTime) + " ms)")
    } catch {
      case e : ExpiredException => warn(e.getMessage)
      case e : InvalidUrlException => chain
    }
  }
    
  private def getSession(req : HttpServletRequest) = {
    val servletSession = req.getSession()
    var session = servletSession.getAttribute("leafs").asInstanceOf[Session]
    if (session == null) {
      session = new Session(server, server.configuration)
      servletSession.setAttribute("leafs", session)
    }
    session 
  }
  
  private def notNull(s : String) = 
    if (s == null) ""
    else s
}

trait LeafsServlet extends HttpServlet with LeafsServletProcessor with Logging with FilterChain {
  override def init(config : ServletConfig) {
    initialize
  }
  
  override def doGet(request : HttpServletRequest, response : HttpServletResponse) {
    super.process(request.asInstanceOf[HttpServletRequest], response, this)
  }
  
  override def doPost(request : HttpServletRequest, response : HttpServletResponse) {
    super.process(request.asInstanceOf[HttpServletRequest], response, this)
  }
  
  override def doFilter(request : ServletRequest, response : ServletResponse) {
    if (response.isInstanceOf[HttpServletResponse]) {
      response.asInstanceOf[HttpServletResponse].sendError(404)
    }
  }
}

trait LeafsFilter extends Filter with LeafsServletProcessor {
  override def init(config : FilterConfig) {
    initialize
  }

  override def doFilter(request : ServletRequest, response : ServletResponse, chain : FilterChain) {
    if (request.isInstanceOf[HttpServletRequest]) 
      super.process(request.asInstanceOf[HttpServletRequest], response, chain)
    else 
      chain.doFilter(request, response)
  }

  override def destroy {
  }
}
