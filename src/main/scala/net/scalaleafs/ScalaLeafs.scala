package net.scalaleafs

import scala.xml.NodeSeq

class ScalaLeafs(root : Class[_ <: Template], val configuration : Configuration) {

  protected def postProcess(request : Request, xml : NodeSeq) = 
    HeadContributions.render(request, xml)
  

}