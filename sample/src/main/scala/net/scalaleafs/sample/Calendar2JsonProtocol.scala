package net.scalaleafs.sample

import spray.json.{JsString, JsValue, RootJsonFormat, DefaultJsonProtocol}
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

object Calendar2JsonProtocol extends DefaultJsonProtocol {

  implicit val eventFormat = jsonFormat7(Event)

  implicit object JodaTimeJsonFormat extends RootJsonFormat[DateTime] {

    def write(datetime: DateTime): JsValue = JsString(ISODateTimeFormat.basicDateTime().print(datetime))

    def read(json: JsValue): DateTime = ISODateTimeFormat.basicDateTime().parseDateTime(json.toString())
  }
}

