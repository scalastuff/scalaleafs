package org.scalastuff.scalaleafs
import java.util.concurrent.ConcurrentHashMap
import scala.io.Source

object Resources {
  private val debugPostfix1 = "// DEBUG MODE";
  private val debugPostfix2 = "/* DEBUG MODE */";
  private val resourceNames = new ConcurrentHashMap[(Class[_], String), String]
  private val resourceData = new ConcurrentHashMap[String, Array[Byte]]

  def resourceContent(resourceName : String) : Array[Byte] = {
    resourceData.get(resourceName)
  }
  
  def hashedNameFor(c : Class[_], name : String) : String = {
    var resourceName = resourceNames.get((c, name))
    if (resourceName == null) {
      try {
        val source = Source.fromInputStream(c.getResourceAsStream(name))
        val linesSeq = for (line : String <- source.getLines) yield {
          if (line.endsWith(debugPostfix1)) 
            if (!R.configuration.debugMode) Seq[String]()
            else Seq(line.dropRight(debugPostfix1.length()))
          else if (line.endsWith(debugPostfix2)) 
            if (!R.configuration.debugMode) Seq[String]()
            else Seq(line.dropRight(debugPostfix2.length()))
          else Seq(line.mkString)
        }
        val lines = linesSeq.flatten
        val data = lines.mkString("\n").getBytes("UTF-8");
        resourceName = hashedName(name, data)
        resourceData.put(resourceName, data);
        if (!R.configuration.debugMode) {
          resourceNames.put((c, name), resourceName)
        }
      } catch {
        case _ => throw new Exception("Resource not found on class-path: " + name)
      }
    }
    resourceName
  }

  private val md = java.security.MessageDigest.getInstance("SHA-1")
  
  private def hashedName(name : String, data : Array[Byte]) = {
    val hash = new sun.misc.BASE64Encoder().encode(md.digest(data))
    val index = name.lastIndexOf('.')
    if (index < 0) name + "-" + hash 
    else name.substring(0, index) + "-" + hash + name.substring(index)  
  }
  
  
}