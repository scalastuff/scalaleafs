package org.scalastuff.scalaleafs;
//package org.scalastuff.scalaleafs
//import java.net.URI
//
//object PageUrl {
//  def apply(path : List[String]) : PageUrl = PageUrl(None, R.baseUri, Nil, path)
//}
//
//case class PageUrl(parent : Option[UrlHandler], baseUri : URI, currentPath : List[String], remainder : List[String]) {
//  lazy val uri = baseUri.resolve((currentPath ++ remainder).mkString("/"))
//  lazy val path = currentPath ++ remainder
//  def child(implicit urlHandler : UrlHandler) : PageUrl = remainder match {
//    case head :: tail => PageUrl(Some(urlHandler), baseUri, currentPath ++ List(head), tail)
//    case Nil => copy(parent = Some(urlHandler))
//  }
//  override lazy val toString = uri.toString
//}
//
///**
// * A page represents a url location. 
// */
//trait UrlHandler {
//  def url : PageUrl
//  implicit val urlHandler = this
//
//  R.requestState.addUrlHandler(url.currentPath, this)
//  
//  def goUrl(path : String) {
//    val fullPath = url.uri.resolve(path).toString
//    if (fullPath.startsWith(url.baseUri.toString)) goUrl(fullPath.substring(url.baseUri.toString.size).split("/").toList)
//    else R.addPostRequestJs(SetWindowLocation(fullPath))
//  }
//  
//  def goUrl(path : List[String]) {
//    val url = UrlHandler.this.url
//    val newUrl = PageUrl(url.parent, url.baseUri, url.currentPath, path.drop(url.currentPath.size))
//    def recurseParent = url.parent match {
//      case Some(parent) => parent.goUrl(path)
//      case None => R.addPostRequestJs(SetWindowLocation(newUrl.uri))
//    }
//    // This page is capable of containing path?
//    if (path.startsWith(url.currentPath)) {
//      // No need to do anything if urls are the same.
//      if (url != newUrl) {
//        // Let page set url.
//        setUrl(newUrl)
//        // Page has handled the url, now do the pushState thing. 
////        if (url == newUrl) {
//          println("registring callback for url " + url)
//          val callback = R.registerAjaxCallback(() => {
//            println("called callback for url " + url)
//            println("  orignal url " + url)
//            setUrl(newUrl)
//          })
//          R.addPostRequestJs(PushState(newUrl.uri.toString, callback))
////        } else recurseParent
//      }
//    } else recurseParent
//  }
//  
//  def setUrl(url : PageUrl) {
//    url.parent match {
//      case Some(parent) => parent.setUrl(url)
//      case None => R.addPostRequestJs(SetWindowLocation(url.uri))
//    }
//  }
//}