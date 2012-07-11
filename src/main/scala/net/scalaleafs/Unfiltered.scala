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
//
//import unfiltered.filter.Planify
//import unfiltered.request.Path
//import unfiltered.request.Seg
//import unfiltered.request.GET
//import unfiltered.response.ResponseString
//import unfiltered.jetty.Http
//import unfiltered.request.HttpRequest
//import org.eclipse.jetty.server.session.SessionHandler

//class WithLeafsUnfilteredHttp(val http : Http) {
//  def withleafs(configuration : Configuration) : Http = withleafs(Nil, configuration) 
//  def withleafs(contextPath : List[String]) : Http = withleafs(contextPath, new Configuration)
//  def withleafs(contextPath : List[String], configuration : Configuration) : Http = {
//    // Jetty started from unfiltered doesn't have a session handler by default.
//    if (http.current.getSessionHandler() == null) {
//      http.current.setSessionHandler(new SessionHandler)
//    }
//    val cfg = configuration
//    http.filter(new ServletFilter {
//      override val configuration = cfg
//    })
//  }
//}