package net.scalaleafs

import scala.concurrent.Future
import scala.xml.NodeSeq
import scala.xml.Elem
import scala.collection.mutable.ArrayBuffer
import scala.xml.Node
import scala.collection.mutable.Queue

/**
 * Enables asynchronous rendering. During rendering, a number of asynchronous operations 
 * might be encountered. They are handled by using a kind of custom continuation method:
 * Rendering is stopped, putting a tagging-element into the rendered xml tree.
 * When the asynchronous operation completes, this element will be replaced with 
 * the rendering of the subtree that depends on the asynchronous result. This process
 * is repeated until all asynchronous operations have been resolved. 
 * <br/>
 * This method of asynchronous processing is more efficient than the alternative: making the
 * entire rendering process asynchronous by default. 
 * There would many composed futures compared to the number of actual asynchronous operations.
 * Especially in the case the library is used only using synchronous processing: there will
 * be no asynchronous processing overhead.    
 */
trait RenderAsync { this : Context =>
  
  private var asyncRenderQueue = Queue[() => Future[(Elem, NodeSeq)]]()
  private var asyncRenderChangesQueue = Queue[() => Future[(NodeSeq, NodeSeq => JSCmd)]]()
  
  /**
   * Adds a asynchronous rendering. All asynchronous renderings will be executed
   * in serial, and never in parallel with normal rendering of the 
   * corresponding window.
   */
  def renderAsync[A](elem : Elem, f : => Future[NodeSeq]) : Elem = {
    asyncRenderQueue += (() => f.map(xml => elem -> xml))
    elem
  }
  
  /**
   * Adds a asynchronous change-rendering. All asynchronous renderings will be executed
   * in serial, and never in parallel with normal rendering of the 
   * corresponding window.
   */
  def renderChangesAsync(f : => Future[NodeSeq], jsCmd : NodeSeq => JSCmd) : JSCmd = {
    asyncRenderChangesQueue += (() => f.map(xml => xml -> jsCmd))
    Noop
  }
  
  /**
   * Returns a future that processes all async renderings in sequence.
   * New async renderings might be added in the process. 
   * The process will continue until async renderings is empty.
   */ 
  private[scalaleafs] def processAsyncRender(xml : NodeSeq) : Future[NodeSeq] = {
    if (asyncRenderQueue.nonEmpty)
      withContext {
        asyncRenderQueue.dequeue()().flatMap {
          case (selection, replacement) =>
            processAsyncRender(replace(xml, selection, replacement))
        }
      }
    else Future.successful(xml)
  }
  
  /**
   * Returns a future that processes all async change-renderings in sequence.
   * Async renderings might be added in the process. 
   * The process will continue until async change-renderings and renderings is empty.
   */ 
  private[scalaleafs] def processAsyncRenderChanges(jsCmd : JSCmd) : Future[JSCmd] = {
    if (asyncRenderChangesQueue.nonEmpty) 
      withContext {
        for {
          (xml, mkJSCmd) <- asyncRenderChangesQueue.dequeue()()
          output <- processAsyncRender(xml)
          cmd = jsCmd & mkJSCmd(output)
          result <- processAsyncRenderChanges(cmd)
        } yield result
      }
    else Future.successful(jsCmd)
  }

  private def replace(xml : NodeSeq, selection : Elem, replacement : NodeSeq) : NodeSeq =
  xml match {
      case elem : Elem if elem eq selection => replacement
      case node : Node => node
      case children =>
        var changed = false
        val result = for {
          child <- children
          nodes = replace(child, selection, replacement)
          _ = changed = changed || child != nodes
          node <- nodes
        } yield node
        if (changed) result
        else children
    }
}