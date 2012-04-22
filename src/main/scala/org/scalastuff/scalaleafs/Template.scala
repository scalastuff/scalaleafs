package org.scalastuff.scalaleafs
import scala.xml.NodeSeq
import java.util.concurrent.ConcurrentHashMap
import scala.xml.XML
import scala.xml.Elem
import java.net.URI

object Template {
  val templateCache = new ConcurrentHashMap[Class[_], NodeSeq]
  def template(c : Class[_]) : NodeSeq = {
    var xml = templateCache.get(c)
    if (xml == null) {
      val resourceName = c.getSimpleName() + ".html";
      try {
        xml = XML.load(c.getResourceAsStream(resourceName))
      } catch {
        case t => throw new Exception("Template not found in class-path: " + resourceName, t)
      }
      if (!R.configuration.debugMode) {
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

/**
 * A Page is a template that outputs a full HTML page. The page is pre- and post-processed. The necessary javascript
 * is added, url's rewritten, head's normalized.
 */
trait PageFrame extends Template {
  
  override def readInput : NodeSeq = {
    val configuration = R.configuration
    val xml = super.readInput

    def augmentHead(head : Elem) = addJavaScriptUrl(head.copy(child = head.child ++ inlineJavaScript))
  
    def inlineJavaScript = if (configuration.debugMode) 
      <script> 
        function callback(uid) {"{"}
          console.log('Callback invoked: /ajaxCallback/' + uid); 
          $.getScript('/ajaxCallback/' + uid);
        {"}"};
      </script> 
    else 
      <script>      
        function callback(uid) {"{"}
          $.getScript('/ajaxCallback/' + uid);
        {"}"};
      </script>
    
    def addJavaScriptUrl(head : Elem) = {
      head.child.find(_.label == "script") match {
        case Some(script : Elem) if XmlHelpers.attr(script, "src").contains("jquery") => head
        case _ => head.copy(child = <script type="text/javascript" src={configuration.jqueryUrl} /> ++ head.child)
      }
    }
    
    def addDebugDiv(html : Elem) = {
      if (!configuration.debugMode) html
      else html.copy(child = html.child ++ 
          <div style="position: absolute; right: 10px; bottom: 10px; border: 1px solid red; color: red; padding: 4px">DEBUG MODE</div>)
    }

    xml match {
      case elem : Elem if elem.label == "html" =>
        val html = addDebugDiv(elem)
        html.child.find(_.label == "head") match {
          case Some(head : Elem) => 
            html.copy(child = html.child.map { 
              case h : Elem if h eq head => augmentHead(head) 
              case h => h
            })
          case None => html.copy(child = augmentHead(<head/>) ++ html.child)
        }
      case _ => xml
    }
  }
  
}