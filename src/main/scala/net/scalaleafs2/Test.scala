package net.scalaleafs2

import spray.routing.Directives
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import implicits._
import akka.actor.ActorSystem

object Test extends App {
  
  
  implicit val actorSystem = ActorSystem("test")
  implicit val config = new Configuration
//  val site = new Site(classOf[PageFrame])
  val server = new SprayServer(classOf[PageFrame])
  server.start
}


class PageFrame extends Template {
  
  val url = window.currentUrl
  
  val render = 
    ".name" #> "Ruud" &
    ".content" #> url.bind { url =>
      ""
    }
  
  override def readInput(context : Context) = 
    <html>
      <h1>Hi there, <span class="name"/>!</h1>
    </html>
}