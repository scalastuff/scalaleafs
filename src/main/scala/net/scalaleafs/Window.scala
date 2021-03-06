package net.scalaleafs

import java.util.UUID

import scala.collection.mutable
import scala.concurrent.Future
import scala.xml.NodeSeq

/**
 * Encapsulates a browser window
 */
class Window(site : Site, initialUrl : Url, rootTemplateInstantiator : RootTemplateInstantiator) {

  import site.executionContext
  private val synchronizedFuture = new SynchronizedFuture
  private val rootTemplate = rootTemplateInstantiator(this)

  private[scalaleafs] val ajaxCallbacks = mutable.HashMap[String, AjaxCallback]()
  private[scalaleafs] var _headContributionKeys : Set[String] = Set.empty
  private[scalaleafs] var _currentUrl : Var[Url] = Var(initialUrl)
  private[scalaleafs] val windowVars = mutable.Map[Any, Var[_]]()
  
  val id : String = "leafs-" + UUID.randomUUID.toString
  
  def handleRequest[A](url : Url, requestVals : RequestVal.Assignment[_]*): Future[String] = {
    import site.executionContext
    synchronizedFuture {
      val context = new Context(site, this, requestVals:_*)
      val initialXml = context.withContext(rootTemplate().render(context, NodeSeq.Empty))
      context.processAsyncRender(initialXml).map(xml => HeadContributions.render(context, xml)).map("<!DOCTYPE html>" + _).andThen {
        case _ => 
          _headContributionKeys ++= context._headContributionKeys
      }
    }
  }

  def handleAjaxCallback(callbackId : String, parameters : Seq[(String, String)], requestVals : RequestVal.Assignment[_]*) : Future[String] = 
    processAjaxCallback(callbackId, parameters).map(jsCmd => site.mkPostRequestJsString(jsCmd.toSeq))
  
  private def processAjaxCallback(callbackId : String, parameters : Seq[(String, String)], requestVals : RequestVal.Assignment[_]*) : Future[JSCmd] = {
    synchronizedFuture {
      ajaxCallbacks.get(callbackId) match {
        case Some(ajaxCallback) => 
          val context = new Context(site, this, requestVals:_*)
          // Call the callback.
          ajaxCallback.f(context)(parameters).flatMap { _ =>
            // Then query the render tree for changes
            val initialChangesJSCmd = rootTemplate().child.renderChanges(context)
            // Process all asynchronous render changes
            context.processAsyncRenderChanges(initialChangesJSCmd).map { changesJSCmd =>
              // Store all current head contributions in the initial request. 
              _headContributionKeys ++= context._headContributionKeys
              // Render additional head contributions.
              val jsHeadContrib = context._headContributions.foldLeft(Noop.asInstanceOf[JSCmd])(_ & _.renderAdditional(context))
              jsHeadContrib & changesJSCmd & context._postRequestJs
            }
          }
        case None => 
          throw new ExpiredException("Callback expired: " + callbackId)
      }
    }
  } 
}

