package net.scalaleafs

import spray.routing.Route
import spray.routing.Directives
import spray.routing.PathMatcher
import shapeless.HNil
import scala.xml.NodeSeq

import java.util.UUID

import spray.http.HttpCookie
import scala.collection.concurrent.TrieMap
import grizzled.slf4j.Logging
import spray.http.MediaTypes
import spray.http.MediaType
import spray.routing.RequestContext

class AbstractSprayServer[R <: Template](root : Class[R], configuration : Configuration) 
  extends Server(root, configuration) with Route with Directives with Logging {
  
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
  
  def apply(v1: spray.routing.RequestContext): Unit = 
    route(v1)
    
  val route = 
    pathPrefix("ui") {
      route2
    }

  def route2 = {
    get {
      println("GET")
      path("kk"/callbackMatcher / Rest) { callbackId =>
        cookie(cookieName) { cookie =>
          complete {
            session.handleAjaxCallback(callbackId, Map.empty)
          }
        }
      } ~
      path("qqleafs" / Rest) { resource =>
      println("RES")
        handleResource(resource) match {
          case Some((bytes, resourceType)) =>
            respondWithMediaType(MediaType.custom(resourceType.contentType)) {
              complete {
                bytes
              }
            }
        }
      } ~
      path(Rest) { path =>
         val (ext, isResource) = extension(path) match {
          case "" => ("", false)
          case "html" => ("html", false)
          case ext => (ext, true)
        }
        val start = System.currentTimeMillis()
        optionalCookie(cookieName) { someCookie =>
          val cookie = someCookie.getOrElse(HttpCookie(cookieName, UUID.randomUUID.toString))
          val session = sessions.getOrElseUpdate(cookie.value, {
            debug(s"Created session: ${cookie.value}")
            new Session(this, configuration)
          })
          setCookie(cookie) {
            if (isResource) {
              val resource = if (path.startsWith("leafs/")) path.substring(6) else path
              handleResource(resource) match {
                case Some((bytes, resourceType)) =>
                  respondWithMediaType(MediaType.custom(resourceType.contentType)) {
                    complete {
                      println("Resource: " + path + " (" + (System.currentTimeMillis - start) + " ms)")
                      bytes
                    }
                  }
                case None =>
                  println("Resource not found: " + path + " (" + (System.currentTimeMillis - start) + " ms)")
                  reject
              }
            }
            else {
              extract(_.request.uri) { uri =>
                val url = new Url(UrlContext(uri.scheme, uri.authority.host.address, uri.authority.port.toString, contextPath), Url.parsePath(path), Map.empty)
                println("url:" + url)
                respondWithMediaType(MediaTypes.`text/html`) {
                  complete {
                    session.handleRequest(url) { 
                      request: Request =>
                        val xml = processRoot(request)
                        println("Processed: " + path + " (" + (System.currentTimeMillis - start) + " ms)")
                        xml
                    } 
                  }
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

  def extension(path: String): String =
    path.lastIndexOf('.') match {
      case -1 => ""
      case i => path.indexOf('/', i) match {
        case -1 => path.substring(i + 1)
        case i => ""
      }
    }
}

