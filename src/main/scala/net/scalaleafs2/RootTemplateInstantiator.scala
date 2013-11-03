package net.scalaleafs2

import scala.io.Source
import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent.TrieMap
import grizzled.slf4j.Logging
import java.util.concurrent.atomic.AtomicLong
import scala.collection.mutable.ArrayBuffer

class RootTemplateInstantiator (rootTemplateClass : Class[_ <: Template], packageName : String, debugMode : Boolean) extends Logging {

  private var loader : DebugClassLoader = null
  
  def apply(window : Window) = {
    var instantiator : Window => Template = null
    var instance : Template = null
    
    () => {
      if (instance == null || (debugMode && isOutdated)) {
        instantiator = reset
        instance = instantiator(window)
      }
      instance
    }
  }
  
  private def reset : Window => Template = {
    synchronized {
      loader = new DebugClassLoader(packageName, rootTemplateClass)
      
      val l = if (debugMode) loader else getClass.getClassLoader 
      val c = l.loadClass(rootTemplateClass.getName) 
      c.getConstructors.find(ctor => ctor.getParameterTypes.size == 1 && ctor.getParameterTypes()(0) == classOf[Window]) match {
        case Some(ctor) => (window : Window) => ctor.newInstance(window).asInstanceOf[Template]
        case None => c.getConstructors.find(ctor => ctor.getParameterTypes.size == 0) match {
          case Some(ctor) => (window : Window) => ctor.newInstance().asInstanceOf[Template]
          case None => throw new Exception("No suitable constructor found for root template class: should get either no parameters or one parameter of type Window")
        }
      }
    }
  }
  
  def isOutdated =
    synchronized {
      if (loader == null || loader.isOutdated) {
        debug("Reloading template classes")
        true
      } 
      else false
    }
  
  if (debugMode) 
    debug("Enabled dynamic classloading for package " + packageName)
}

class DebugClassLoader(packageName : String, rootTemplateClass : Class[_ <: Template]) extends ClassLoader {
  
  private val crcs = new TrieMap[String, Int]
  
  def isOutdated = 
    crcs.foldLeft(false)((outdated, entry) => outdated || checksum(entry._1) != Some(entry._2))
  
  override def loadClass(name : String) = {
    val c = findLoadedClass(name) 
    if (c.eq(null) || c.eq(rootTemplateClass)) {
      if (name.startsWith(packageName)) {
        val classFile = name.replace('.', '/') + ".class"
        read(classFile) match {
          case Some((bytes, crc)) =>
            crcs.put(classFile, crc)
            defineClass(name, bytes, 0, bytes.size)
          case None => super.loadClass(name)
        }
      } else {
        super.loadClass(name);
      }
    }
    else {
      c
    }
  }
  
  private def read(classFile : String) : Option[(Array[Byte], Int)] = {
    getParent.getResource(classFile) match {
      case null =>
        None
      case resource =>
        val is = resource.openStream
        try {
          var a = 1
          var b = 0
          val buffer = ArrayBuffer[Byte]()
          var data = is.read
          while (data != -1) {
            val byte = data.toByte
            buffer += byte
            a = (byte + a) % MOD_ADLER
            b = (b + a) % MOD_ADLER
            data = is.read
          }
          val bytes = buffer.toArray
          val crc = b * 65536 + a
          Some(bytes, crc)
        } 
        finally {
          is.close
        }
    }
  }

  private def checksum(classFile : String) : Option[(Int)] = {
    getParent.getResource(classFile) match {
      case null =>
        None
      case resource =>
        val is = resource.openStream
        try {
          var a = 1
          var b = 0
          var data = is.read
          while (data != -1) {
            val byte = data.toByte
            a = (byte + a) % MOD_ADLER
            b = (b + a) % MOD_ADLER
            data = is.read
          }
          val crc = b * 65536 + a
          Some(crc)
        } 
        finally {
          is.close
        }
    }
  }

  val MOD_ADLER = 65521

//  def adler32sum(bytes : Array[Byte]): Int = {
//    var a = 1
//    var b = 0
//    bytes.foreach(char => {
//      a = (char + a) % MOD_ADLER
//      b = (b + a) % MOD_ADLER
//    })
//    // note: Int is 32 bits, which this requires
//    return b * 65536 + a     // or (b << 16) + a
//  }
}