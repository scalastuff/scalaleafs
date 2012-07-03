package org.scalastuff.scalaleafs
import scala.xml.NodeSeq
import java.util.concurrent.ConcurrentHashMap
import scala.xml.XML
import scala.xml.Elem
import java.net.URI
import org.xml.sax.SAXParseException

object Template {
  val templateCache = new ConcurrentHashMap[Class[_], NodeSeq]
  def template(c : Class[_]) : NodeSeq = {
    var xml = templateCache.get(c)
    if (xml == null) {
      val resourceName = c.getName().replace('.', '/') + ".html";
      try {
        val is = c.getClassLoader.getResourceAsStream(resourceName)
        if (is == null) {
          throw new Exception("Template not found on classpath: " + resourceName)
        }
        xml = XML.load(is) match {
          case elem : Elem if elem.label == "dummy" => elem.child
          case xml => xml
        }
      } catch {
        case t : SAXParseException if t.getLineNumber >= 0 => throw new Exception("Error in template " + resourceName + " (line " + t.getLineNumber + "): " + t.getLocalizedMessage, t)
        case t => throw new Exception("Error in template " + resourceName + ": " + t, t)
      }
      if (!R.debugMode) {
        templateCache.put(c, xml)
      }
    }
    xml
  }
}

/**
 * A template is an XmlTransformation that reads its input, by default, from a class-path resource, and provides
 * a bind hook to transform this input to some output.
 * Class-path resources are cached (when not in debug mode) in a JVM-global cache.
 */
trait Template extends XmlTransformation {
  def bind : NodeSeq => NodeSeq
  def readInput : NodeSeq = Template.template(getClass)
  val input = readInput
  def render : NodeSeq = apply(input)
  abstract override def apply(xml : NodeSeq) = bind(super.apply(input))
}

