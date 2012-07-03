package org.scalastuff.scalaleafs
import java.util.concurrent.ConcurrentHashMap
import scala.io.Source
import javax.xml.bind.DatatypeConverter

object ResourceType {
  private val map = Map(
    "js" -> ResourceType("js", "text/javascript", Some("UTF-8")),
    "css" -> ResourceType("css", "text/css", Some("UTF-8")),
    "png" -> ResourceType("js", "image/png", Some("UTF-8")),
    "js" -> ResourceType("js", "text/javascript", Some("UTF-8")),
    "js" -> ResourceType("js", "text/javascript", Some("UTF-8")),
    "js" -> ResourceType("js", "text/javascript", Some("UTF-8"))
  )
  def get(extention : String) = map.get(extention)
  def of(resourceName : String) : ResourceType = {
    val index = resourceName.lastIndexOf('.');
    if (index <= 0) ResourceType("", "binary", None)
    else {
      val extention = resourceName.substring(index + 1)
      get(extention) match {
        case Some(resourceType) => resourceType
        case None => ResourceType(extention, "binary", None)
      }
    }
  }
}

case class ResourceType(extention: String, contentType : String, encoding : Option[String])

object Resources {
  private val debugPostfix1 = "// DEBUG MODE";
  private val debugPostfix2 = "/* DEBUG MODE */";
  private val resourcePaths = new ConcurrentHashMap[(Class[_], String), String]
  private val resourceData = new ConcurrentHashMap[String, (Array[Byte], ResourceType)]

  def resourceContent(resourcePath : String) : Option[(Array[Byte], ResourceType)] = {
    val result = resourceData.get(resourcePath)
    if (result != null) Some(result)
    else {
      val resourceType = ResourceType.of(resourcePath)
      val stream = classOf[Server].getClassLoader.getResourceAsStream(resourcePath)
      if (stream != null) {
        try {
          val bytes = Stream.continually(stream.read).takeWhile(-1 !=).map(_.toByte).toArray
          val result = (bytes, resourceType)
          resourceData.put(resourcePath, result)
          Some(result)
        } finally {
          stream.close()
        }
      } else {
        // Do not cache negative results, users may cause memory overflows.
        None
      }
    }
  }
  
  def hashedResourcePathFor(c : Class[_], name : String) : String = {
    var resourcePath = resourcePaths.get((c, name))
    if (resourcePath == null) {
      val rootPackage = if (name.startsWith("/")) R.session.server.resourceRoot else c.getPackage
      val fullName = rootPackage.getName.replace('.', '/') + (if (name.startsWith("/")) "" else "/") + name
      try {
        val resourceType = ResourceType.of(name)
        // Text resource?
        val bytes = resourceType.encoding match {
          case Some(encoding) =>
            val source = Source.fromInputStream(c.getResourceAsStream(name))
            val linesSeq = source.getLines map { line =>
              line.replace("$$CONTEXT", R.server.contextPath.mkString("/"))
            } map { line =>
              if (line.endsWith(debugPostfix1)) 
                if (!R.debugMode) Seq[String]()
                else Seq(line.dropRight(debugPostfix1.length()))
              else if (line.endsWith(debugPostfix2)) 
                if (!R.debugMode) Seq[String]()
                else Seq(line.dropRight(debugPostfix2.length()))
              else Seq(line.mkString)
            } 
            val lines = linesSeq.flatten
            lines.mkString("\n").getBytes("UTF-8");
          case None =>
            Array[Byte]()
        }
        resourcePath = hashedName(fullName, bytes)
        resourceData.put(resourcePath, (bytes, resourceType));
        if (!R.debugMode) {
          resourcePaths.put((c, name), resourcePath)
        }
      } catch {
        case _ => throw new Exception("Resource not found on class-path: " + fullName)
      }
    }
    resourcePath
  }

  private val md = java.security.MessageDigest.getInstance("SHA-1")
  
  private def hashedName(name : String, data : Array[Byte]) = {
    val hash = DatatypeConverter.printBase64Binary(md.digest(data)).reverse
    val index = name.lastIndexOf('.')
    if (index < 0) name + "-" + hash 
    else name.substring(0, index) + "-" + hash + name.substring(index)  
  }
  
  
}