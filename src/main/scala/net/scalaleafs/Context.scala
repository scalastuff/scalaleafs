package net.scalaleafs

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.xml.Elem
import scala.xml.NodeSeq

class Context(val site : Site, val window : Window, requestVals : RequestVal.Assignment[_]*)(implicit val executionContext : ExecutionContext) 
    extends RenderAsync with Callbacks with HeadContributions {

  private[scalaleafs] var _postRequestJs : JSCmd = Noop
  private[scalaleafs] val requestVars = mutable.Map[Any, Var[_]](requestVals.map(t => t._1 -> Var[Any](t._2)):_*)

  def debugMode = site.debugMode

  /**
   * Sends the javascript command to the browser upon completion of the current request
   */
  def addPostRequestJs(jsCmd : JSCmd) {
    if (jsCmd != Noop) {
      _postRequestJs &= jsCmd
    }
  }
  
  def url : Url = window._currentUrl.get
  
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

  private[scalaleafs] def popUrl(uri : String) = {
    val url = window._currentUrl.get.resolve(uri)
    if (window._currentUrl.get != url) {
      window._currentUrl.set(url)
    }
  }
  private[scalaleafs] def withContext[A](f : => A) : A = {
//    val previous = Context.get
//    Context.set(this)
//    try f
//    finally Context.set(previous)
    f
  }
}
