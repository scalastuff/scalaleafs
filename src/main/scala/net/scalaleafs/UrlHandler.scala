package net.scalaleafs


import collection.mutable

/**
 * Implement this trait to be able to react on url changes. Handlers are called when a url is changed in 
 * an ajax callback. It allows for url changes without page switches. 
 */
trait UrlHandler {
  def tail : Var[UrlTail]
  def handleUrl(tail : UrlTail) {
    this.tail.set(tail)
  }
  R.initialRequest.urlManager.addUrlHandler(this)
  R.addHeadContribution(OnPopStateHeadContribution)
}

class UrlManager {
  private var urlHandlers : mutable.Map[(UrlContext, Seq[String]), UrlHandler] = null
  
  private[scalaleafs] def addUrlHandler(handler : UrlHandler) {
    if (urlHandlers == null) {
      urlHandlers = new mutable.HashMap[(UrlContext, Seq[String]), UrlHandler]()
    }
    val tail = handler.tail.get
    urlHandlers.put((tail.context, tail.url), handler)
  }
  
 def handleUrl(url : Url) : JSCmd = {
    var result : Option[JSCmd] = None 
    if (urlHandlers != null) {
      var currentPath = url.path
      var remainingPath : List[String] = Nil
      var stop = false;
      do {
        if (currentPath.isEmpty) stop = true;
        handleUrl(url.context, url, currentPath, remainingPath, url.parameters) match {
          case Some(cmd) => 
            result = Some(cmd)
          case None => 
            if (currentPath.nonEmpty) {
              remainingPath = currentPath.last +: remainingPath
              currentPath = currentPath.dropRight(1)
            }
        }
        
      } while (result == None && !stop)
    }
    result match {
      case Some(cmd) => cmd
      case None => Noop//SetWindowLocation(url)
    }
  }
  
  private def handleUrl(context : UrlContext, url : Url, currentPath : Seq[String], remainingPath : List[String], parameters : Map[String, Seq[String]]) : Option[JSCmd] = {
    urlHandlers.get((context, currentPath)) match {
      case Some(handler) => 
        val tail = new UrlTail(url, remainingPath)
        // Only need to handle if url is, in fact, different.
        if (tail != handler.tail.get) {
          // Try handle the url.
          handler.handleUrl(tail)
          // Url was handled?
          if (tail == handler.tail.get) {
            Some(JSCmd("window.history.pushState(\"" + url + "\", '" + "title" + "', '" + url + "');"))
          }
          // Url not handled, try next handler.
          else None
        }
        // Nothing to do if url hasn't changed
        else Some(Noop) 
        // No handler found, try next handler.
      case None => None
      }
  }
}
