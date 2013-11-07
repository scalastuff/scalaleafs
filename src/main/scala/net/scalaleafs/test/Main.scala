package net.scalaleafs.test

import net.scalaleafs._
import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Main extends App {

  
  implicit val actorSystem = ActorSystem("test")
  implicit val config = new Configuration(
    SprayServerContextPath -> List("bla")
  )

  
//  implicit val ec = actorSystem.dispatcher
//  for (i <- 0 to 40000000) {
//    val f = Future.successful(i)
//    val g = f.map(_ * 2)
//
//    result += 2
//  }
//  Thread.sleep(3000)
//  val start = System.currentTimeMillis
//  var result = 0
//  for (i <- 0 to 1000000) {
//    val f = Future.successful(i)
//    val g = f.map(_ * 2)
//
//    result += 2
//  }
//  
//  println("G: " + result + " " + (System.currentTimeMillis - start) + " ms")
//  
  
  val site = new Site(classOf[Frame], List("bla"), config)(actorSystem.dispatcher) 
  val server = new SprayServer(classOf[Frame], config, actorSystem)
  server.start
}

case class SecurityContext(user : String)


//object SecurityContextVar extends ContextVar[SecurityContext]


class PageFrame(window : Window) extends Template {
  
  
  val url = CurrentUrl
  
  val render = bind(url) { u =>
    ".name" #> ("url:" + u) &
    "#clickme" #> setAttr("href", "println") & 
    "#clickme" #> setText("click me not") & 
    "#clickme" #> onclick(println("I HAVE BEEN CLICKED") & Noop) & 
    ".content" #> bind(url) { url =>
      ""
    }
  }
  
  override val input = 
    <html>
      <h1>Hi there, <span class="name"/>!</h1>
      <a id="clickme">click me</a>
    </html>
}