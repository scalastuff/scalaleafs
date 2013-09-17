package net.scalaleafs

import spray.routing.Route
import spray.routing.Directives
import spray.routing.PathMatcher
import shapeless.HNil
import spray.http.MediaType
import scala.xml.NodeSeq

class ScalaLeafsSprayRoute(server : Server)(render : UrlTrail => NodeSeq) extends Route with Directives {
  
  private val nilMatcher : PathMatcher[HNil] = ""
  private val contextMatcher  = matcherOf(server.contextPath)
  private val callbackMatcher = matcherOf(server.ajaxCallbackPath)
  private val formPostMatcher = matcherOf(server.ajaxFormPostPath)
  private val resourceMatcher = matcherOf(server.resourcePath)
  private val session : Session = null
  
  def matcherOf(path : Seq[String]) = path.foldLeft(nilMatcher)((x, y) => x / y)
  def matcherOf(path : String) : PathMatcher[HNil] = matcherOf(path.split("/").toSeq)
  
  protected def postProcess(request : Request, xml : NodeSeq) = HeadContributions.render(request, xml)

  def apply(v1: spray.routing.RequestContext): Unit = 
    route(v1)
 
  val route = pathPrefix(contextMatcher) {
    get {
      path(callbackMatcher / Rest) { callbackId =>
        complete {
          session.handleAjaxCallback(callbackId, Map.empty)
        }
      } ~
      path(resourceMatcher / Rest) { resource =>
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
        extract(_.request.uri) { uri =>
        val url = new Url(UrlContext(uri.scheme, uri.authority.host.address, uri.authority.port.toString, server.contextPath), path :: Nil, Map.empty)
          complete {
            session.handleRequest(url) { 
              request: Request =>
                postProcess(request, render(UrlTrail(url, Url.parsePath(path))))
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
}

