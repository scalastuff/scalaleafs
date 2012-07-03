//package org.scalastuff.scalaleafs
//
//import javax.servlet.Filter
//import javax.servlet.FilterConfig
//import javax.servlet.ServletRequest
//import javax.servlet.ServletResponse
//import javax.servlet.FilterChain
//import java.net.URLClassLoader
//import java.net.URL
//
//trait AbstractLeafsFilter extends Filter {
//
//  val debugMode = System.getProperty("leafs.debugMode").equals("true")
//  val useDebugDelegate = debugMode && getClass.getClassLoader.isInstanceOf[URLClassLoader] && !getClass.getClassLoader.isInstanceOf[DebugClassLoader]
//  var delegate : Option[AbstractLeafsFilter] = None;
//  
//  var debugClassLoader : Option[DebugClassLoader]
//  
//  abstract override def init(config : FilterConfig) {
//    super.init(config)
//    
//    // Create a delegate?
//    if (useDebugClassLoader) {
//      if (delegate == None) {
//        
//      }
//    }
//    if (debugMode && !getClass.getClassLoader.isInstanceOf[DebugClassLoader]) {
//      
//      // Was this not already the debug class-loader?
//      val cl = getClass.getClassLoader
//      if (!getClass.getClassLoader.isInstanceOf[DebugClassLoader]) {
//        
//        // Can only operate when class-loader is a url classloader
//        if (cl.isInstanceOf[URLClassLoader]) {
//          
//        }
//        
//      }
//    }
//  }
//    
//  
//  abstract override def destroy {
//    super.destroy
//  }
//
//  abstract override def doFilter(request : ServletRequest, response : ServletResponse, chain : FilterChain) {
//    
//  }
//  
//}
//
//class DelegateFilter(delegateClassName : String, originalClassLoader : URLClassLoader) extends Filter {
//  var delegate : Option[Any] = None
//  private var _classLoader : Option[DebugClassLoader] = None
//  
//  
//  def classLoader = _classLoader match {
//    case Some(cl) => cl
//    case None => new DebugClassLoader(originalClassLoader)
//  } 
//}
//
//
//
//class DebugClassLoader(originalClassLoader : URLClassLoader) extends URLClassLoader {
//  
//}