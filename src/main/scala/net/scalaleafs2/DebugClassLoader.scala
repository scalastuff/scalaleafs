package net.scalaleafs2

import scala.io.Source
import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent.TrieMap

class DebugClassLoaderInstantiator(packageName : String) {
  private var loader : DebugClassLoader = null
  
  def instantiate[A](c : Class[A]) : A = {
    if (loader == null || loader.isOutdated) {
      loader = new DebugClassLoader(packageName)
    }
    loader.loadClass(c.getName).newInstance.asInstanceOf[A]
  }
  
  def isOutdated = 
    loader == null || loader.isOutdated
}

class DebugClassLoader(packageName : String) extends ClassLoader {
  
  private val crcs = new TrieMap[String, Int]
  
  private val processorClassName =
    classOf[DebugClassLoader].getName
  
  def isOutdated = {
    crcs.foldLeft(false)((outdated, entry) => outdated || read(entry._1).map(_._2) != Some(entry._2))
  }
  
  override def loadClass(name : String) = {
    findLoadedClass(name) match {
      case null =>
        if (name.startsWith(packageName) || name == processorClassName) {
          read(name) match {
            case Some((bytes, crc)) =>
              crcs.put(name, crc)
              defineClass(name, bytes, 0, bytes.size)
            case None => super.loadClass(name)
          }
        } else {
          super.loadClass(name);
        }
      case c => c
    }
  }
  
  private def read(name : String) : Option[(Array[Byte], Int)] = {
    getParent.getResource(name.replace('.', '/') + ".class") match {
      case null =>
        None
      case resource =>
        val is = resource.openStream
        try {
          val bytes = Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray
          val crc = adler32sum(bytes)
          Some(bytes, crc)
        } 
        finally {
          is.close
        }
    }
  }
  
  val MOD_ADLER = 65521

  def adler32sum(bytes : Array[Byte]): Int = {
    var a = 1
    var b = 0
    bytes.foreach(char => {
      a = (char + a) % MOD_ADLER
      b = (b + a) % MOD_ADLER
    })
    // note: Int is 32 bits, which this requires
    return b * 65536 + a     // or (b << 16) + a
  }
}