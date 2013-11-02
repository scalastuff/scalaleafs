package net.scalaleafs2

import scala.concurrent.Future

trait Callbacks { this : Context =>
  
  def callback(f : => Unit) : JSCmd = 
    callbackAsync(Future.successful(f))
  
  def callback(f : Map[String, Seq[String]] => Unit) : JSCmd = 
    callbackAsync(p => Future.successful(f(p)))
    
  def callback(parameter : JSExp)(f : String => Unit) : JSCmd = 
    callbackAsync(parameter)(p => Future.successful(f(p)))
  
  def callback(parameters : (String, JSExp)*)(f : Map[String, Seq[String]] => Unit) : JSCmd = 
    callbackAsync(parameters:_*)(p => Future.successful(f(p)))

  def callbackAsync(f : => Future[Unit]) : JSCmd = 
    callbackAsync()(_ => f)
  
  def callbackAsync(f : Map[String, Seq[String]] => Future[Unit]) : JSCmd = 
    callbackAsync()(f)
    
  def callbackAsync(parameter : JSExp)(f : String => Future[Unit]) : JSCmd = 
    JSCmd("leafs.callback('" + callbackIdAsync(m => f(m.get("value").flatMap(_.headOption).getOrElse(""))) + "?value=' + encodeURIComponent(" + parameter.toString + "));")
  
  def callbackAsync(parameters : (String, JSExp)*)(f : Map[String, Seq[String]] => Future[Unit]) : JSCmd = 
    if (parameters.isEmpty)
      JSCmd("leafs.callback('" + callbackIdAsync(f) + "');")
    else 
      JSCmd("leafs.callback('" + callbackIdAsync(f) + "?" + parameters.map(x => x._1.toString + "=' + encodeURIComponent(" + x._2.toString + ")").mkString(" + '&") + ");")

  private[scalaleafs2] def callbackId(f : Map[String, Seq[String]] => Unit) : String = 
    callbackIdAsync(p => Future.successful(f(p)))
    
  private[scalaleafs2] def callbackIdAsync(f : Map[String, Seq[String]] => Future[Unit]) : String = {
    val uid = site.generateCallbackID
    window.ajaxCallbacks.put(uid, AjaxCallback(f))
    addHeadContribution(JQuery)
    addHeadContribution(LeafsJavaScriptResource)
    uid
  }
}