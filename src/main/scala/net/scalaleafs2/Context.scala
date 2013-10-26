package net.scalaleafs2

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class Context(val site : Site, window : Window)(implicit val executionContext : ExecutionContext) {

  private[scalaleafs2] var _headContributionKeys : Set[String] = Set.empty
  private[scalaleafs2] var _headContributions : Seq[HeadContribution] = Seq.empty
  private[scalaleafs2] var _postRequestJs : JSCmd = JsNoop

  def debugMode = true
  
  def annotation : Any = null
  
  def headContributions = 
    _headContributions
    
  def addHeadContribution(contribution : HeadContribution) {
    val key = contribution.key
    if (!window._headContributionKeys.contains(key)) {
      if (!_headContributionKeys.contains(key)) {
        
        // Add key first to prevent cyclic behavior.
        _headContributionKeys += key
        
        // Add dependencies before actual contribution.
        contribution.dependsOn.foreach(dep => addHeadContribution(dep))
        
        // Now add actual contribution.
        _headContributions :+= contribution
      }
    }
  }
  
  /**
   * Sends the javascript command to the browser upon completion of the current request
   */
  def addPostRequestJs(jsCmd : JSCmd) {
    if (jsCmd != JsNoop) {
      _postRequestJs &= jsCmd
    }
  }
  
  def url : Var[Url] = window._currentUrl
  
    /**
   * Changes the browser url without a page refresh.
   */
  def url_=(uri : String) : Unit = 
    url_=(window._currentUrl.get.resolve(uri))

  /**
   * Changes the browser url without a page refresh.
   */
  def url_=(url : Url) : Unit = {
    if (window._currentUrl.get != url) {
      window._currentUrl.set(url)
      addPostRequestJs(JSCmd("window.history.pushState(\"" + url + "\", '" + "title" + "', '" + url + "');"))
    }
  }

  def callback(parameter : JSExp)(f : String => Future[Unit]) = 
    JSCmd("leafs.callback('" + callbackId(m => f(m.get("value").flatMap(_.headOption).getOrElse(""))) + "?value=' + encodeURIComponent(" + parameter.toString + "));")
  
  def callback(f : Map[String, Seq[String]] => Future[Unit]) : JSCmd = 
    callback()(f)
  
  def callback(parameters : (String, JSExp)*)(f : Map[String, Seq[String]] => Future[Unit]) : JSCmd = 
    if (parameters.isEmpty)
      JSCmd("leafs.callback('" + callbackId(f) + "');")
    else 
      JSCmd("leafs.callback('" + callbackId(f) + "?" + parameters.map(x => x._1.toString + "=' + encodeURIComponent(" + x._2.toString + ")").mkString(" + '&") + ");")
  
  
  private[scalaleafs2] def popUrl(uri : String) = {
    val url = window._currentUrl.get.resolve(uri)
    if (window._currentUrl.get != url) {
      window._currentUrl.set(url)
    }
  }
  
  private[scalaleafs2] def callbackId(f : Map[String, Seq[String]] => Future[Unit]) : String = {
    val uid = site.generateCallbackID
    window.ajaxCallbacks.put(uid, AjaxCallback(f))
    addHeadContribution(JQuery)
    addHeadContribution(LeafsJavaScriptResource)
    uid
  }
  
}

trait ContextAnnotationFactory extends Function[Context, Any]
