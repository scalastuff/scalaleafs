package net.scalaleafs2

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.xml.Elem
import scala.xml.NodeSeq

class Context(val site : Site, val window : Window)(implicit val executionContext : ExecutionContext) 
    extends RenderAsync with Callbacks with HeadContributions {

  private[scalaleafs2] var _postRequestJs : JSCmd = JSNoop

  def debugMode = true
  
  def annotation : Any = null
  
  
  /**
   * Sends the javascript command to the browser upon completion of the current request
   */
  def addPostRequestJs(jsCmd : JSCmd) {
    if (jsCmd != JSNoop) {
      _postRequestJs &= jsCmd
    }
  }
  
  def url : Val[Url] = window._currentUrl
  
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

  private[scalaleafs2] def popUrl(uri : String) = {
    val url = window._currentUrl.get.resolve(uri)
    if (window._currentUrl.get != url) {
      window._currentUrl.set(url)
    }
  }
}

trait ContextAnnotationFactory extends Function[Context, Any]
