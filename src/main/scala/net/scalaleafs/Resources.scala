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

import java.util.concurrent.ConcurrentHashMap
import scala.io.Source
import javax.xml.bind.DatatypeConverter
import java.io.InputStream
import java.io.BufferedInputStream

/**
 * Resource meta data.
 */
case class ResourceType(extention: String, contentType : String, encoding : Option[String])

object ResourceType {
  private val map = Map(
    "js" -> ResourceType("js", "text/javascript", Some("UTF-8")),
    "css" -> ResourceType("css", "text/css", Some("UTF-8")),
    "png" -> ResourceType("png", "image/png", None),
    "js" -> ResourceType("js", "text/javascript", Some("UTF-8"))
  )
  def get(extention : String) = map.get(extention)
  def of(resourceName : String) : ResourceType = {
    val index = resourceName.lastIndexOf('.')
    if (index <= 0) ResourceType("", "application/octet-stream", None)
    else {
      val extention = resourceName.substring(index + 1)
      get(extention) match {
        case Some(resourceType) => resourceType
        case None => ResourceType(extention, "application/octet-stream", None)
      }
    }
  }
}

/**
 * Trait through which resources can be loaded. Separates resource handling from the environment the application is deployed in.
 */
trait ResourceFactory {
  def getResource(name : String) : Option[InputStream]
}

object ResourceFactory extends ConfigVal[ResourceFactory](NullResourceFactory)

object NullResourceFactory extends ResourceFactory {
  def getResource(name : String) : Option[InputStream] = None
}

object ClasspathResourceFactory {
  def apply(cls : Class[_], path : String) : ClasspathResourceFactory =
    new ClasspathResourceFactory(cls.getClassLoader, cls.getPackage.getName.replace('.', '/') + '/' + path)
}

class ClasspathResourceFactory(loader : ClassLoader, path : String) extends ResourceFactory {
    def getResource(name : String) : Option[InputStream] = 
      Option(loader.getResourceAsStream(path + "/" + name))
}

/**
 * Entry point for resources. It processes (e.g. string replacements) and caches resource data.
 * There is typically one instance per server.
 */
class Resources(factory : ResourceFactory, substitutions : Map[String, String], debugMode : Boolean = false) {
  private val debugPostfix1 = "// DEBUG MODE"
  private val debugPostfix2 = "/* DEBUG MODE */"
  private val resourcePaths = new ConcurrentHashMap[(Class[_], String), String]
  private val resourceData = new ConcurrentHashMap[String, (Array[Byte], ResourceType)]
  private val resourceFactoryClass = factory.getClass

  def resourceContent(resourcePath : String) : Option[(Array[Byte], ResourceType)] = {
    assert (!resourcePath.startsWith("/"))
    resourceData.get(resourcePath) match {
      case null =>
        factory.getResource(resourcePath) match {
          case Some(is) => 
            val (_, resourceType, bytes) = readHashedResource(resourcePath, is, substitutions)
            val result = (bytes, resourceType)
            resourceData.put(resourcePath, result)
            Some(result)
          case None =>
            // Do not cache negative results, users may cause memory overflow.
            None
        }
      case result =>
        Some(result)
    }
  }
  
  def hashedResourcePathFor(c : Class[_], name : String) : String = {
    assert (!name.startsWith("/"))
    resourcePaths.get((c, name)) match {
      case null =>
        val fullName = "/" + c.getPackage.getName.replace('.', '/') + "/" + name
        val (resourcePath, resourceType, bytes) = readHashedResource(name, c.getResourceAsStream(fullName), substitutions)
        assert (!resourcePath.startsWith("/"))
        resourceData.put(resourcePath, (bytes, resourceType))
        if (!debugMode) {
          resourcePaths.put((c, name), resourcePath)
        }
        resourcePath
      case path => path
    }
  }

  def hashedResourcePathFor(name : String) : String = {
    assert (!name.startsWith("/"))
    resourcePaths.get((resourceFactoryClass, name)) match {
      case null =>
        val (resourcePath, resourceType, bytes) = readHashedResource(name, factory.getResource(name).getOrElse(null), substitutions)
        resourceData.put(resourcePath, (bytes, resourceType))
        if (!debugMode) {
          resourcePaths.put((resourceFactoryClass, name), resourcePath)
        }
        resourcePath
      case path => path
    }
  }
  
  def readHashedResource(name : String, is : InputStream, substitutions : Map[String, String]) : (String, ResourceType, Array[Byte]) = {
    if (is == null) {
      throw new Exception("Resource not found: " + name)
    }
    try {
      val resourceType = ResourceType.of(name)
      // Text resource?
      val bytes = resourceType.encoding match {
        case Some(encoding) =>
          val source = Source.fromInputStream(is, "UTF-8") // TODO determine input encoding correctly
          val linesSeq = source.getLines map { line =>
            substitutions.foldLeft(line)((line, subst) => line.replace("$$" + subst._1, subst._2))
          } map { line =>
            if (line.endsWith(debugPostfix1)) 
              if (!debugMode) Seq[String]()
              else Seq(line.dropRight(debugPostfix1.length()))
            else if (line.endsWith(debugPostfix2)) 
              if (!debugMode) Seq[String]()
              else Seq(line.dropRight(debugPostfix2.length()))
            else Seq(line.mkString)
          } 
          val lines = linesSeq.toSeq.flatten.mkString("\n")
          lines.getBytes("UTF-8")
        case None =>
          val bis = new BufferedInputStream(is)
          Stream.continually(bis.read).takeWhile(-1 != _).map(_.toByte).toArray
      }
      (hashedName(name, bytes), resourceType, bytes)
    } catch {
      case e : Throwable => throw new Exception("Resource could not be read: " + name, e)
    } finally {
      is.close()
    }
  }

  private val md = java.security.MessageDigest.getInstance("SHA-1")
  
  private def hashedName(name : String, data : Array[Byte]) = {
    val hash = DatatypeConverter.printBase64Binary(md.digest(data)).reverse.replace('/', '_')
    val index = name.lastIndexOf('/')
    if (index < 0) hash + "_" + name 
    else name.substring(0, index) + "/" + hash + "_" + name.substring(index + 1)  
  }
}