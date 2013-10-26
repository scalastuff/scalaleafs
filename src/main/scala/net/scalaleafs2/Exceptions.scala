package net.scalaleafs2


class ExpiredException(message : String) extends Exception(message)
class InvalidUrlException(url : Url) extends Exception("Invalid url: " + url)
