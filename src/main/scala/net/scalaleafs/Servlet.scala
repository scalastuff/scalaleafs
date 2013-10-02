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

import java.io.InputStream

import javax.servlet.ServletContext

import net.scalaleafs.Configuration;
import net.scalaleafs.ContextPath;
import net.scalaleafs.HeadContributions;
import net.scalaleafs.ResourceFactory;
import net.scalaleafs.Server;
import net.scalaleafs.Session;
import net.scalaleafs.Url;

trait LeafsServletProcessor extends Logging {

  private var context : ServletContext = null
  private var server : Server = null
  private var ajaxCallbackPrefix = ""
  private var ajaxFormPostPrefix = ""
  private var resourcePrefix = ""
  protected val configuration : Configuration
  protected def render(tail : UrlTail) : NodeSeq
  protected def postProcess(request : Request, xml : NodeSeq) = HeadContributions.render(request, xml)

  def initialize(context : ServletContext) {
    this.context = context
  }
  
  protected def process(request : HttpServletRequest, response : ServletResponse, chain : FilterChain) {
    if (server == null) {
      val webAppResourceFactory = new ResourceFactory {
          def getResource(name : String) : Option[InputStream] = {
            Option(context.getResourceAsStream(if (name.startsWith("/")) name else "/" + name))
          }
      }
      val contextPath = Url.parsePath(request.getContextPath)
      server = new Server(null/*TODO*/, configuration.withDefaults(ResourceFactory -> webAppResourceFactory, ContextPath -> contextPath))
      ajaxCallbackPrefix = server.ajaxCallbackPath.mkString("/", "/", "/")
      ajaxFormPostPrefix = server.ajaxFormPostPath.mkString("/", "/", "")
      resourcePrefix = server.resourcePath.mkString("/", "/", "/")
    }
    try {
      val startTime = System.currentTimeMillis
      val session = getSession(request)
        val path = 
          if (request.getPathInfo == null) request.getServletPath()
          else request.getPathInfo
      println("Context path: " + request.getContextPath())
      println("Servlet path: " + request.getServletPath())
      println("Path Info: " + request.getPathInfo())
      println("Path: " + path)
      val parameters : Map[String, Seq[String]] = request.getParameterMap().toMap.map(kv => kv._1.toString -> kv._2.asInstanceOf[Array[String]].toSeq)
      if (request.getMethod == "GET" && path.startsWith(ajaxCallbackPrefix)) {
        val js = session.handleAjaxCallback(path.substring(ajaxCallbackPrefix.length), parameters)
        response.getWriter.write(js)
        response.flushBuffer
      }
      else if (request.getMethod == "POST" && path.startsWith(ajaxFormPostPrefix)) {
        val js = session.handleAjaxFormPost(parameters)
            response.getWriter.write(js)
            response.flushBuffer
      } 
      else if (request.getMethod == "GET" && path.startsWith(resourcePrefix)) {
        server.handleResource(path.substring(resourcePrefix.length)) match {
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
        val ps = Url.parsePath(path)
        val url = new Url(urlContext, ps, parameters)
        session.handleRequest(url) { request =>
          val xml = postProcess(request, render(UrlTail(url)))
          val outputString = xml.toString
          response.setContentType("text/html")
          response.getWriter.append(outputString);
          response.flushBuffer;
        }
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
  
  protected val configuration = new Configuration
  
  override def init(config : ServletConfig) {
    initialize(config.getServletContext())
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
  
  protected val configuration = new Configuration 
  
  override def init(config : FilterConfig) {
    initialize(config.getServletContext())
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
