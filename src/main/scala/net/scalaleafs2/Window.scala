package net.scalaleafs2

import java.util.UUID

import scala.collection.mutable
import scala.concurrent.Future
import scala.xml.NodeSeq

/**
 * Encapsulates a browser window
 */
class Window(site : Site, url : Url, rootTemplateInstantiator : RootTemplateInstantiator) {

  import site.executionContext
  private val synchronizedFuture = new SynchronizedFuture
  private val rootTemplate = rootTemplateInstantiator(this)
  
  private[scalaleafs2] val ajaxCallbacks = mutable.HashMap[String, AjaxCallback]()
  private[scalaleafs2] var _headContributionKeys : Set[String] = Set.empty
  private[scalaleafs2] var _currentUrl : Var[Url] = Var(url)
  
  def url : SyncVal[Url] = _currentUrl
  val id : String = "leafs-" + UUID.randomUUID.toString
  
  def handleRequest[A](url : Url): Future[NodeSeq] = {
    import site.executionContext
    synchronizedFuture {
      val context = new Context(site, this)
      rootTemplate().renderAsync(context).map(xml => HeadContributions.render(context, xml)).andThen {
        case _ =>
          _headContributionKeys ++= context._headContributionKeys
      }
    }
  }

  def handleAjaxCallback(callbackId : String, parameters : Map[String, Seq[String]]) : Future[String] = 
    processAjaxCallback(callbackId, parameters).map(jsCmd => site.mkPostRequestJsString(jsCmd.toSeq))
  
  private def processAjaxCallback(callbackId : String, parameters : Map[String, Seq[String]]) : Future[JSCmd] = {
    synchronizedFuture {
      ajaxCallbacks.get(callbackId) match {
        case Some(ajaxCallback) => 
          val context = new Context(site, this)
          // Call the callback.
          ajaxCallback.f(parameters).flatMap { _ =>
            // Then query the render tree for changes
            rootTemplate().renderChangesAsync(context).map  { jsChanges =>
              // Store all current head contributions in the initial request. 
              _headContributionKeys ++= context._headContributionKeys
              // Render additional head contributions.
              val jsHeadContrib = context._headContributions.foldLeft(JsNoop.asInstanceOf[JSCmd])(_ & _.renderAdditional(context))
              jsHeadContrib & jsChanges & context._postRequestJs
            }
          }
        case None => 
          throw new ExpiredException("Callback expired: " + callbackId)
      }
    }
  } 
}

