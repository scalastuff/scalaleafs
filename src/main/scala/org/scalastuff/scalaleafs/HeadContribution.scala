package org.scalastuff.scalaleafs

import scala.collection.mutable
import org.scalastuff.scalaleafs.implicits._
import scala.xml.NodeSeq
import scala.xml.Elem

object HeadContributions {
  
  def render(request : TransientRequest, xml : NodeSeq) : NodeSeq = {

    def addDebugDiv(html : Elem) =
      if (!request.configuration.debugMode) html
      else html.copy(child = html.child ++ 
          <div style="position: absolute; right: 10px; bottom: 10px; border: 1px solid red; color: red; padding: 4px">DEBUG MODE</div>)

    def addHeadContributions(html : Elem, contributions : Iterable[HeadContribution]) = {
          
      def add(head : Elem) = head.copy(child = head.child ++ contributions.flatMap(_.render(request)))
  
      if (contributions.isEmpty) html
      else html.child.find(_.label == "head") match {
        case Some(head : Elem) => 
          html.copy(child = html.child.map { 
            case h : Elem if h eq head => add(head) 
            case h => h
          })
        case None => html.copy(child = add(<head/>) ++ html.child)
      }
    }

    xml match {
      case elem : Elem =>
        if (elem.label == "html") {
          val html = addDebugDiv(elem)
          addHeadContributions(html, request.headContributions)
        } else elem
      case Seq(elem : Elem) => render(request, elem)
      case _ => xml
    }
  }
}

trait HeadContributions extends XmlTransformation {
  abstract override def apply(xml : NodeSeq) : NodeSeq = {
   HeadContributions.render(R, super.apply(xml)) 
  }
}

abstract class HeadContribution(val key : String) {
  def dependsOn : List[HeadContribution] = Nil
  def render(request : TransientRequest) : NodeSeq
}

class JavaScript(key : String, uri : String) extends HeadContribution(key) {
  def render(request : TransientRequest) = {
    <script type="text/javascript" src={uri} />
  }
}

class JavaScriptResource(c : Class[_], resource : String) extends HeadContribution(c.getName + "/" + resource) {
  var name = Resources.hashedNameFor(c, resource)
  def render(request : TransientRequest) = {
    if (request.configuration.debugMode) {
      name = Resources.hashedNameFor(c, resource)
    }
    <script type="text/javascript" src={request.session.server.resourceUrl(name)} />
  }
}

object LeafsJavaScriptResource extends JavaScriptResource(classOf[JavaScript], "leafs.js")

object JQuery extends HeadContribution("jquery") {
  def render(request : TransientRequest) = {
    <script type="text/javascript" src={request.configuration.jqueryUrl} />
  }
}

object Callback extends HeadContribution("callback") {
  override def dependsOn = JQuery :: LeafsJavaScriptResource :: Nil
  def render(request : TransientRequest) = NodeSeq.Empty
}

object OnPopState extends HeadContribution("onPopState") {
  override def dependsOn = Callback :: LeafsJavaScriptResource :: Nil
  def render(request : TransientRequest) = {
    val callbackId = R.registerAjaxCallback( _.get("url") match {
      case Some(location :: Nil) => 
        if (location.startsWith("pop:")) R.popUrl(location.substring(4))
        else R.url = location
      case _ => 
    })
    <script>      
      function setUrl(url) {"{"}
        callback('{callbackId}?url=' + url);
      {"}"};
    </script>
  }
}

