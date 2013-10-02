package net.scalaleafs

/**
 * An initial request is created for each http request but shared for each subsequent callback.
 */
class InitialRequest(val session : Session, val configuration : Configuration, private[scalaleafs] var _url : Url) {
  private[scalaleafs] var _headContributionKeys : Set[String] = Set.empty
  private[scalaleafs] val urlManager = new UrlManager
  
  lazy val resourceBase = _url.context.copy(path = _url.context.path ++ session.server.resourcePath)
}
