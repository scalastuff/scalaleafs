package org.scalastuff.scalaleafs.demo

import scala.xml.NodeSeq

import net.scalaleafs.Configuration
import net.scalaleafs.HeadContributions
import net.scalaleafs.LeafsServlet
import net.scalaleafs.Template
import net.scalaleafs.UrlHandler
import net.scalaleafs.UrlTrail
import net.scalaleafs.Var
import net.scalaleafs.Var.fromInitialValue

class DemoApplication extends LeafsServlet {
  val configuration = new Configuration()
  def render(trail : UrlTrail) : NodeSeq = {
    new DemoPageFrame(trail).render;
  }
}

class DemoPageFrame(val trail : Var[UrlTrail]) extends Template with HeadContributions with UrlHandler {
  val bind = (xml : NodeSeq) => NodeSeq.Empty
}