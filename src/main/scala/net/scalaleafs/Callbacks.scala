package net.scalaleafs

import scala.concurrent.Future

trait Callbacks { this : Context =>
    
  def callback(f : Context => Seq[(String, String)] => Unit) : JSCmd = 
    callbackAsync()(c => p => Future.successful(f(c)(p)))
    
  def callback(parameter : JSExp)(f : Context => String => Unit) : JSCmd = 
    callbackAsync(parameter)(c => p => Future.successful(f(c)(p)))
  
  def callback(parameters : (String, JSExp)*)(f : Context => Seq[(String, String)] => Unit) : JSCmd = 
    callbackAsync(parameters:_*)(c => p => Future.successful(f(c)(p)))

  def callbackAsync(f : Context => Seq[(String, String)] => Future[Unit]) : JSCmd = 
    callbackAsync()(c => p => f(c)(p))
    
  def callbackAsync(parameter : JSExp)(f : Context => String => Future[Unit]) : JSCmd = 
    JSCmd("leafs.callback('" + callbackIdAsync(c => pars => f(c)(pars.find(_._1 == "value").map(_._2).getOrElse(""))) + "?value=' + encodeURIComponent(" + parameter.toString + "));")
  
  def callbackAsync(parameters : (String, JSExp)*)(f : Context => Seq[(String, String)] => Future[Unit]) : JSCmd = 
    if (parameters.isEmpty)
      JSCmd("leafs.callback('" + callbackIdAsync(f) + "');")
    else 
      JSCmd("leafs.callback('" + callbackIdAsync(f) + "?" + parameters.map(x => x._1.toString + "=' + encodeURIComponent(" + x._2.toString + ")").mkString(" + '&") + ");")

  private[scalaleafs] def callbackId(f : Context => Seq[(String, String)] => Unit) : String = 
    callbackIdAsync(p => c => Future.successful(f(p)(c)))
    
  private[scalaleafs] def callbackIdAsync(f : Context => Seq[(String, String)] => Future[Unit]) : String = {
    val uid = site.generateCallbackID
    window.ajaxCallbacks.put(uid, AjaxCallback(f))
    addHeadContribution(JQuery)
    addHeadContribution(LeafsJavaScriptResource)
    uid
  }
}