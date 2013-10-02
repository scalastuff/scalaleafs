package net.scalaleafs.sample.ui

import spray.can.Http
import spray.http.HttpRequest
import spray.routing.RoutingSettings
import spray.routing.RejectionHandler
import spray.routing.Route
import spray.routing.HttpServiceActor
import spray.routing.HttpService
import spray.routing.directives.LogEntry
import spray.routing.ExceptionHandler
import akka.io.IO
import spray.routing.Directives
import akka.actor.ActorSystem
import akka.event.Logging.DebugLevel
import akka.actor.Props

class SprayServer(route : Route) extends HttpService with Directives {

  lazy implicit val actorSystem = ActorSystem("ScalaleafsSample")
  def actorRefFactory = actorSystem

  val interface : String = "localhost"
  val port : Int = 8080
  val exceptionHandler : ExceptionHandler = ExceptionHandler.default
  def rejectionHandler : RejectionHandler = RejectionHandler.Default
  val routingSettings = RoutingSettings(actorSystem)

  private implicit def eh = exceptionHandler
  private implicit val rh = rejectionHandler 
  private implicit def rs = routingSettings

  class Actor extends HttpServiceActor {
    val theRoute = route
    def receive = runRoute(logRequest(showRequest _) { theRoute })
    def showRequest(request: HttpRequest) = LogEntry("URL: " + request.uri + "\n CONTENT: " + request.entity, DebugLevel)

  }

  def start =
    IO(Http) ! Http.Bind(actorSystem.actorOf(Props(new Actor), "http-server"), interface = interface, port = port)
}
