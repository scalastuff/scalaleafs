package net.scalaleafs.contrib

import net.scalaleafs.NoChildRenderNode
import net.scalaleafs.RenderNode
import scala.xml.NodeSeq
import net.scalaleafs.Context
import net.scalaleafs.HeadContribution
import net.scalaleafs.ExpectElemWithIdRenderNode
import scala.xml.Elem

object CalendarJavascriptResource extends HeadContribution("Calendar") {
  def render(context : Context) = {
    <script type="text/javascript" src="http://cdnjs.cloudflare.com/ajax/libs/fullcalendar/1.6.4/fullcalendar.js"></script>
  }
}

class Calendar extends ExpectElemWithIdRenderNode with NoChildRenderNode {

  def render(context : Context, elem : Elem, id : String) = {
    context.addHeadContribution(CalendarJavascriptResource)
    elem
  }
}