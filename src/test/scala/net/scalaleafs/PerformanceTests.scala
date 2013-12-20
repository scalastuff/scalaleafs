package net.scalaleafs

import java.util.concurrent.ConcurrentHashMap
import scala.util.Random


object PerformanceTests {

  def main(args: Array[String]): Unit = {
    
    var c = 0
    val map = new ConcurrentHashMap[String, String]
    for (i <- 1 to 1000000) map.put(Random.nextString(8), "value")
    time("ConcurrentHashMap lookup", 1000000) {
      map.get("some string") match {
        case null => c += 1
        case s => c += 2
      }
    }
    
    val threadLocal = new ThreadLocal[String]
    time("ThreadLocal lookup", 1000000000) {
      threadLocal.get()
    }
    
  }

  
  def time(name : String, times : Integer)(f : => Unit) {
    val start = System.currentTimeMillis()
    var i = 0
    while (i < times) {
      f
      i += 1
    }
    val end = System.currentTimeMillis()
    System.out.println(name + " (" + times + " times) took " + (end - start) + " msec")
  }
}