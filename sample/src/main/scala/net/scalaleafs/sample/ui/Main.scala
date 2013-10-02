package net.scalaleafs.sample.ui

import net.scalaleafs._

object Main extends App {

  val config = new Configuration()
  val leafs = new AbstractSprayServer(classOf[Index], config.withDefaults(ContextPath -> List("ui"))) 
  val server = new SprayServer(leafs)
  server.start
}