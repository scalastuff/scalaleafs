package net.scalaleafs.sample

import net.scalaleafs._
import spray.json._
import Calendar2JsonProtocol._
import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}
import org.joda.time.DateTime

object Calendar2JavaScriptResource extends JavaScriptLibrary("jquery.calendar.js", "jquery.calendar.js")

case class Event(uid : String, begins : DateTime, ends : DateTime, color : String, resource : String, title : String, notes : Option[String])

class Calendar2 extends Template {
  val today = DateTime.now().withTimeAtStartOfDay()
  val newEvent = Event("100", today.plusHours(15), today.plusHours(16), "#dddddd", "90", "After Click Event", None)
  val events = Val(
      Event("10", today.plusHours(11), today.plusHours(12), "#dddddd", "90", "First Event", None) ::
      Event("11", today.plusHours(12).plusMinutes(15), today.plusHours(13).plusMinutes(45), "#dddddd", "16", "Second Event", Some("Some Notes")) :: Nil)


  def render =
    contrib(Calendar2JavaScriptResource) & exec(JSCmd("initCal()")) &
    "#calendar2" #>
      onclick(JSCmd("$('#calendar2').cal('add'," + newEvent.toJson + ")")) &
  bind(events) { events =>
          exec(JSCmd("$('#calendar2').cal('clear', 'fast', 'linear')"))
        } &
  bindAll(events) { event =>
        exec(JSCmd("$('#calendar2').cal('add'," + event.get.toJson + ")"))
      }

//  def calendar(events: Val[List[Event]], onClick: Int => Unit = _ => ()) =
//    bindAll(events) { event =>
//
//    }
//
//  def render2 =
//  "#calendar" #> calendar(events)
}