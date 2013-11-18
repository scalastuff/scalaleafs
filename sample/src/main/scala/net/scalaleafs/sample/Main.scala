package net.scalaleafs.sample

import akka.actor.ActorSystem
import net.scalaleafs._

object Main extends App {

  val actorSystem = ActorSystem("ScalaLeafsSample")
  val config = new Configuration(
      ResourceFactory -> new ClasspathResourceFactory(classOf[Frame].getClassLoader, "public"))
  val server = new SprayServer(classOf[Frame], config, actorSystem)
  server.start
}