package net.scalaleafs


/**
 * A tail is a path to a url. Starting at a context, one can advance the tail to the end, where the
 * tail reached its url. Typically, advancing a tail to its end corresponds with the way pages are rendered.
 */
class UrlTail private[scalaleafs] (val totalUrl : Url, val remainingPath : List[String]) {
  
  def context = 
    totalUrl.context
    
  def url = 
    totalUrl.path.dropRight(remainingPath.size)

  override def toString = 
    remainingPath.mkString("/")
}

object UrlTail {
  
  def apply(url : Url) : UrlTail = 
    new UrlTail(url, url.path.toList)

  def unapply(tail : UrlTail) : Option[(String, UrlTail)] = {
    if (tail.remainingPath.isEmpty) None
    else Some((tail.remainingPath.head, new UrlTail(tail.totalUrl, tail.remainingPath)))
  }
}