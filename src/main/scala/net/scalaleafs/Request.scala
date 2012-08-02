/**
 * Copyright (c) 2012 Ruud Diterwich.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package net.scalaleafs

/**
 * An initial request is created for each http request but shared for each subsequent callback.
 */
class InitialRequest(val session : Session, val configuration : Configuration, private[scalaleafs] var _url : Url) {
  private[scalaleafs] var _headContributionKeys : Set[String] = Set.empty
  private[scalaleafs] val urlManager = new UrlManager
  
  lazy val resourceBaseUrl = Url(_url.context, session.server.resourcePath, Map.empty)
}

/**
 * A request is created for each http request, including both the initial page request and the subsequent callback calls.
 */
class Request(val initialRequest : InitialRequest, val isInitialRequest : Boolean) {
  var eagerPostRequestJs : JSCmd = Noop
  var postRequestJs : JSCmd = Noop
  private[scalaleafs] var _headContributionKeys : Set[String] = Set.empty
  private[scalaleafs] var _headContributions : Seq[HeadContribution] = Seq.empty

  def configuration = initialRequest.configuration
  def session = initialRequest.session
  def server = initialRequest.session.server
  def resourceBaseUrl = initialRequest.resourceBaseUrl
  def debugMode = session.server.debugMode
  
  /**
   * The current request url.
   */
  def url = initialRequest._url
  
  /**
   * Changes the browser url without a page refresh.
   */
  def changeUrl(uri : String) : Unit = changeUrl(initialRequest._url.resolve(uri))

  /**
   * Changes the browser url without a page refresh.
   */
  def changeUrl(url : Url) : Unit = {
    if (initialRequest._url != url) {
      initialRequest._url = url
      addPostRequestJs(initialRequest.urlManager.handleUrl(url))
    }
  }
  private[scalaleafs] def popUrl(uri : String) = {
    val url = initialRequest._url.resolve(uri)
    if (initialRequest._url != url) {
      initialRequest._url = url
      initialRequest.urlManager.handleUrl(url)
    }
  }

  def headContributions = 
    _headContributions
    
  def addHeadContribution(contribution : HeadContribution) {
    val key = contribution.key
    if (!initialRequest._headContributionKeys.contains(key)) {
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
 
  def addPostRequestJs(JSCmd : JSCmd) {
    if (JSCmd != Noop) {
      postRequestJs &= JSCmd
    }
  }

  def addEagerPostRequestJs(JSCmd : JSCmd) {
    if (JSCmd != Noop) {
      eagerPostRequestJs &= JSCmd
    }
  }
  
  def callbackId(f : Map[String, Seq[String]] => Unit) : String = {
      val uid = session.callbackIDGenerator.generate
      initialRequest.session.ajaxCallbacks.put(uid, AjaxCallback(initialRequest, f))
      addHeadContribution(JQuery)
      addHeadContribution(LeafsJavaScriptResource)
      uid
  }
  
  // Deprecated: see if flatMap works
  def callback1(f : String => Unit, parameter : JSExp) = {
    JSCmd("leafs.callback('" + callbackId(m => f(m.get("value").getOrElse(Seq.empty).headOption.getOrElse(""))) + "?value=' + " + parameter.toString + ");")
  }
  
  def callback(parameter : JSExp)(f : String => Unit) = {
    JSCmd("leafs.callback('" + callbackId(m => f(m.get("value").flatMap(_.headOption).getOrElse(""))) + "?value=' + " + parameter.toString + ");")
  }
  
  def callback(f : Map[String, Seq[String]] => Unit) : JSCmd = {
    callback()(f)
  }
  
  def callback(parameters : (String, JSExp)*)(f : Map[String, Seq[String]] => Unit) : JSCmd = {
      if (parameters.isEmpty)
        JSCmd("leafs.callback('" + callbackId(f) + "');")
      else 
        JSCmd("leafs.callback('" + callbackId(f) + "?" + parameters.map(x => x._1.toString + "=' + " + x._2.toString).mkString(" + '&") + ");")
  }
}

/**
 * Global entry point to ScalaLeafs context. R returns the current request, from which the current InitialRequest,
 * the Session and the Server can be reached.
 */
object R extends ThreadLocal[Request] {
  implicit def toRequest(r : ThreadLocal[Request]) : Request = r.get
  
  /**
   * Overridden to throw an exception if thread local hasn't been set.
   */
  override def get = super.get match {
    case null => throw new Exception("No request context")
    case transientRequest => transientRequest
  }
}

