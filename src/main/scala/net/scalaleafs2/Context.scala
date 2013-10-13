package net.scalaleafs2

import scala.concurrent.ExecutionContext

class Context {

  def debugMode = true
  implicit def executionContext : ExecutionContext = null
}