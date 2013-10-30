package net.scalaleafs2.test

import spray.routing.Directives
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import net.scalaleafs2._
import akka.actor.ActorSystem

object Test extends App {
  
  
  implicit val actorSystem = ActorSystem("test")
  implicit val config = new Configuration(
    SprayServerContextPath -> List("bla")
  )
//  val site = new Site(classOf[PageFrame])
  val server = new SprayServer(classOf[PageFrame], config, actorSystem)
  server.start
}


class PageFrame(window : Window) extends Template {
  
  val url = window.url
  
  val render = url.bind { u =>
    ".name" #> ("url:" + u) &
    "#clickme" #> setAttr("href", "println") & 
    "#clickme" #> setText("click me not") & 
    "#clickme" #> onclick(println("I HAVE BEEN CLICKED")) & 
    ".content" #> url.bind { url =>
      ""
    }
  }
  
  override def readInput(context : Context) = 
    <html>
      <h1>Hi there, <span class="name"/>!</h1>
      <a id="clickme">click me</a>
    </html>
}