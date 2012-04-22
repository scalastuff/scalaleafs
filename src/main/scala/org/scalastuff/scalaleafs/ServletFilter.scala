package org.scalastuff.scalaleafs

import unfiltered.filter.Planify
import unfiltered.request.Path
import unfiltered.request.Seg
import unfiltered.request.GET
import unfiltered.response.ResponseString
import unfiltered.jetty.Http
import unfiltered.request.HttpRequest
import javax.servlet.http.HttpServletRequest
import javax.servlet.Filter
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import unfiltered.filter.Plan
import unfiltered.response.Pass
import javax.servlet.ServletConfig
import java.net.URI
import scala.collection.JavaConversions._
import org.eclipse.jetty.server.session.SessionHandler

class WithLeafsUnfilteredHttp(val http : Http) {
  def withleafs(contextPath : List[String] = Nil, configuration : Configuration = Configuration()) = {
    if (http.current.getSessionHandler() == null) {
      http.current.setSessionHandler(new SessionHandler)
    }
    val cfg = configuration
    http.filter(new ServletFilter {
      override val configuration = cfg
    })
  }
}

trait leafsFilter extends Filter {
  var server : Server = null
  var ajaxCallbackPrefix = ""
  var resourcePrefix = ""

  val configuration = Configuration()  
    
  abstract override def init(config : FilterConfig) {
    super.init(config)
    server = new Server(configuration)
    ajaxCallbackPrefix = server.configuration.contextPath.mkString("/") + "/" + Server.ajaxCallbackPath + "/"
    resourcePrefix = server.configuration.contextPath.mkString("/") + "/" + Server.resourcePath + "/"
  }
  abstract override def destroy {
    super.destroy
  }

  abstract override def doFilter(request : ServletRequest, response : ServletResponse, chain : FilterChain) {
    if (request.isInstanceOf[HttpServletRequest]) {
      val startTime = System.currentTimeMillis
      val httpRequest = request.asInstanceOf[HttpServletRequest];
      val session = getSession(httpRequest)
      val servletPath = httpRequest.getServletPath()
      val parameters : Map[String, List[String]] = httpRequest.getParameterMap().toMap.map(kv => kv._1.toString -> kv._2.asInstanceOf[Array[String]].toList)
      if (servletPath.startsWith(ajaxCallbackPrefix)) {
        val js = session.handleAjaxCallback(servletPath.substring(ajaxCallbackPrefix.length), parameters)
        response.getWriter().write(js)
        response.flushBuffer()
      } else if (servletPath.startsWith(resourcePrefix)) {
        val js = session.handleResource(servletPath.substring(resourcePrefix.length))
        response.getOutputStream().write(js)
        response.flushBuffer()
      } else {
        val baseUri = new URI(request.getScheme(), null, request.getServerName(), request.getLocalPort(), httpRequest.getContextPath() + "/", null, null)
        val path = (httpRequest.getServletPath() + httpRequest.getPathInfo()).split("/").toList
        session.handleRequest(new Url(baseUri, Nil, path, parameters), () => super.doFilter(request, response, chain))
      }
      println("Processed " + httpRequest.getRequestURI() + " (" + (System.currentTimeMillis- startTime) + " ms)")
    } else {
      super.doFilter(request, response, chain)
    }
  }
    
  private def getSession(req : HttpServletRequest) = {
    val servletSession = req.getSession()
    var session = servletSession.getValue("leafs").asInstanceOf[Session]
    if (session == null) {
      session = new Session(server, server.configuration)
      servletSession.putValue("leafs", session)
    }
    session 
  }
}

class DefaultServletFilter extends Filter {
    
  def init(config : FilterConfig) {}
  def destroy {}
  def doFilter(request : ServletRequest, response : ServletResponse, chain : FilterChain) {
    chain.doFilter(request, response)
  }
}

class ServletFilter extends DefaultServletFilter with leafsFilter
