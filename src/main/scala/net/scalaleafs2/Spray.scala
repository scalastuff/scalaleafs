package net.scalaleafs2

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
import scala.concurrent.Future

class SprayRoute(site : Site) extends Route with Directives with Logging {
  
  private val nilMatcher : PathMatcher[HNil] = ""
  private val contextMatcher  = matcherOf(site.contextPath)
  private val callbackMatcher = matcherOf(site.ajaxCallbackPath)
  private val formPostMatcher = matcherOf(site.ajaxFormPostPath)
  private val resourceMatcher = matcherOf(site.resourcePath)
  private val sessions = TrieMap[String, Session]()  
  private val session: Session = null  

  private val cookieName = "scalaleafs"
    
  def matcherOf(path : Seq[String]) = path.foldLeft(nilMatcher)((x, y) => x / y)
  def matcherOf(path : String) : PathMatcher[HNil] = matcherOf(path.split("/").toSeq)
  
  def apply(v1: spray.routing.RequestContext): Unit = 
    route(v1)
    
  val route0 = 
    pathPrefix("ui") {
      route
    }

  import site.executionContext
  def route = {
    get {
      println("GET")
      path("kk"/callbackMatcher / Rest) { callbackId =>
        cookie(cookieName) { cookie =>
          respondWithMediaType(MediaTypes.`application/json`) {
            complete {
              site.handleAjaxCallback("windowId", callbackId, Map.empty)
            }
          }
        }
      } ~
      path("qqleafs" / Rest) { resource =>
      println("RES")
        site.handleResource(resource) match {
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
            new Session//(this, configuration)
          })
          setCookie(cookie) {
            if (isResource) {
              val resource = if (path.startsWith("leafs/")) path.substring(6) else path
              site.handleResource(resource) match {
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
                val url = new Url(UrlContext(uri.scheme, uri.authority.host.address, uri.authority.port.toString, site.contextPath), Url.parsePath(path), Map.empty)
                println("url:" + url)
                respondWithMediaType(MediaTypes.`text/html`) {
                  complete {
                    import site.executionContext
                    site.handleRequest(url).map { 
                      xml: NodeSeq =>
//                        val xml = processRoot(request)
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
//        site.handleAjaxFormPost(Map.empty)
        ""
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

import akka.actor.ActorSystem
import akka.actor.Props
import akka.event.Logging.DebugLevel
import akka.io.IO
import spray.can.Http
import spray.http.HttpRequest
import spray.routing.Directive.pimpApply
import spray.routing.HttpService.pimpRouteWithConcatenation
import spray.routing.HttpServiceActor
import spray.routing.Route
import spray.routing.RoutingSettings
import spray.routing.directives.LogEntry
import spray.routing.ExceptionHandler
import spray.routing.HttpService
import spray.routing.RejectionHandler

object SprayServerPort extends ConfigVar[Int](80)
object SprayServerInterface extends ConfigVar[String]("0.0.0.0")
object SprayServerContextPath extends ConfigVar[List[String]](Nil)

class SprayServer(val site : Site)(implicit actorSystem : ActorSystem, configuration : Configuration) extends HttpService with Logging {

  def this(rootTemplateClass : Class[_ <: Template])(implicit actorSystem : ActorSystem, configuration : Configuration) = 
    this(new Site(rootTemplateClass, SprayServerContextPath)(actorSystem.dispatcher, configuration))
  
  val exceptionHandler : ExceptionHandler = ExceptionHandler.default
  def rejectionHandler : RejectionHandler = RejectionHandler.Default
  val routingSettings = RoutingSettings(actorSystem)

  private implicit def eh = exceptionHandler
  private implicit val rh = rejectionHandler 
  private implicit def rs = routingSettings

  val route = new SprayRoute(site)
  val actorRefFactory = actorSystem

  class Actor extends HttpServiceActor {
    val theRoute = route
    def receive = runRoute(logRequest(showRequest _) { theRoute })
    def showRequest(request: HttpRequest) = LogEntry("URL: " + request.uri + "\n CONTENT: " + request.entity, DebugLevel)

  }

  def start =
    IO(Http) ! Http.Bind(actorSystem.actorOf(Props(new Actor), "http-server"), interface = SprayServerInterface, port = SprayServerPort)
}

object SprayServer {
  
}
