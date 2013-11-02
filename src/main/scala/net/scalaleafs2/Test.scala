package net.scalaleafs2.test

import spray.routing.Directives
import scala.concurrent.Future
import net.scalaleafs2._
import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._

object Test extends App {

  
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
  

  
  //  val site = new Site(classOf[PageFrame])
  val server = new SprayServer(classOf[PageFrame], config, actorSystem)
  server.start
}


class PageFrame(window : Window) extends Template {
  
  val url = window.url
  
  val url2 = url.map(implicit context => x => XX)
  
  val url3 = Var()
  
  def XX(implicit executionContext : ExecutionContext) = "XX"
  
  val render = url.bind { u =>
    ".name" #> ("url:" + u) &
    "#clickme" #> setAttr("href", "println") & 
    "#clickme" #> setText("click me not") & 
    "#clickme" #> onclick(println("I HAVE BEEN CLICKED")) & 
    ".content" #> url.bind { url =>
      ""
    }
  }
  
  override val input = 
    <html>
      <h1>Hi there, <span class="name"/>!</h1>
      <a id="clickme">click me</a>
    </html>
}