package net.scalaleafs

import spray.routing.Directives
import spray.routing.Route
import spray.routing.PathMatcher
import shapeless.HNil
import spray.http.MediaType
import scala.xml.NodeSeq
import java.util.UUID
import spray.http.HttpCookie
import scala.collection.concurrent.TrieMap
import grizzled.slf4j.Logging


abstract class AbstractSprayServer(configuration : Configuration) 
  extends Server(Nil, configuration) with Route with Directives with Logging {
  
  private val nilMatcher : PathMatcher[HNil] = ""
  private val contextMatcher  = matcherOf(contextPath)
  private val callbackMatcher = matcherOf(ajaxCallbackPath)
  private val formPostMatcher = matcherOf(ajaxFormPostPath)
  private val resourceMatcher = matcherOf(resourcePath)
  private val sessions = TrieMap[String, Session]()  
  private val session: Session = null  

  private val cookieName = "scalaleafs"
  
  def matcherOf(path : Seq[String]) = path.foldLeft(nilMatcher)((x, y) => x / y)
  def matcherOf(path : String) : PathMatcher[HNil] = matcherOf(path.split("/").toSeq)
  
  protected def postProcess(request : Request, xml : NodeSeq) = HeadContributions.render(request, xml)

  def apply(v1: spray.routing.RequestContext): Unit = 
    route(v1)
    
  val myroute = 
    pathPrefix("ui") {
      route
    }
 
  val route = pathPrefix("") {
    get {
      path(callbackMatcher / Rest) { callbackId =>
      println("GET")
        cookie(cookieName) { cookie =>
          complete {
            session.handleAjaxCallback(callbackId, Map.empty)
          }
        }
      } ~
      path(resourceMatcher / Rest) { resource =>
      println("RES")
        session.handleResource(resource) match {
          case Some((bytes, resourceType)) =>
            respondWithMediaType(MediaType.custom(resourceType.contentType)) {
              complete {
                bytes
              }
            }
        }
      } ~
      path(Rest) { path =>
        println("path:"+path)
        optionalCookie(cookieName) { someCookie =>
          val cookie = someCookie.getOrElse(HttpCookie(cookieName, UUID.randomUUID.toString))
          val session = sessions.getOrElseUpdate(cookie.value, {
            debug(s"Created session: ${cookie.value}")
            new Session(this, configuration)
          })
          setCookie(cookie) {
            extract(_.request.uri) { uri =>
            val url = new Url(UrlContext(uri.scheme, uri.authority.host.address, uri.authority.port.toString, contextPath), path :: Nil, Map.empty)
            println("url:" + url)
              complete {
                session.handleRequest(url) { 
                  request: Request =>
                    postProcess(request, render(UrlTrail(url, Url.parsePath(path))))
                }
              }
            }
          }
        }
      }
    } ~
    post {
      complete {
        session.handleAjaxFormPost(Map.empty)
      }
    }
  }
    
  protected def render : UrlTrail => NodeSeq
}

