package net.scalaleafs2

import scala.xml.NodeSeq
import scala.xml.Elem
import scala.concurrent.Future
import scala.collection.mutable.ArrayBuffer
import scala.xml.Node


/**
 * An Selector is used to match elements in an XML structure. When recursive is set, the xml is searched recursively. 
 * Otherwise (the default) only the top-level nodes are searched. When a nested selector is specified, child nodes of
 * a matched element are searched using the nested selector.
 */
class Selector(val matches : Elem => Boolean, val recursive : Boolean = false, val nested : Option[Selector] = None) 

object Selector {
  /**
   * Utility method that replaces elements of given xml that match according to given selector. 
   */
  def transform(context : Context, xml : NodeSeq, selector : Selector, replace : (Context, Elem) => NodeSeq) : NodeSeq = {
    var changed = false
    var builder = NodeSeq.newBuilder
    xml foreach {
      case elem : Elem if selector.matches(elem) => 
        selector.nested match {
          case Some(nested) =>
            val recursed = transform(context, elem.child, nested, replace)
            // Optimization: Don't clone when child list is the same instance
            if (!(recursed eq elem.child)) {
              changed = true;
              builder += elem.copy(child = recursed)
            } else {
              builder += elem
            }
          case None =>
            val replacement = replace(context, elem)
            builder ++= replacement
            changed = true
        }
      case elem : Elem if selector.recursive => 
        val recursed = transform(context, elem.child, selector, replace)
        // Optimization: Don't clone when child list is the same instance
        if (!(recursed eq elem.child)) {
          changed = true;
          builder += elem.copy(child = recursed)
        } else {
          builder += elem
        }
      case node => 
        builder += node
    }
    // Optimization: Make sure the same node list is returned when nothing changed.
    if (changed) builder.result
    else xml
  }

  val changed = <h1>Changed</h1>
  val unchanged = <h1>Unchanged</h1>
  
}

class SynchronousSelectorRenderNode(selector : Selector, child : SyncRenderNode) extends SyncRenderNode {
  override def render(context : Context, xml : NodeSeq) = 
    Selector.transform(context, xml, selector, child.render)
} 

/**
 * Render node that uses a selector to transform xml.
 */
class SelectorRenderNode(selector : Selector, child : RenderNode) extends RenderNode {
  
  var elements = ArrayBuffer[Elem]()
  
  override def renderAsync(context : Context, xml : NodeSeq) : Future[NodeSeq] = {
    import context.executionContext
    
    elements.clear

    try {
      def findMatchedElements(xml : NodeSeq, selector : Selector) : Boolean = {
        xml match {
          case elem : Elem =>
            val index = elements.size
            elements += Selector.unchanged
            val matches = selector.matches(elem) 
            if (matches && selector.nested == None) {
              elements(index) = elem
              false
            }
            else {
              if (findMatchedElements(elem.child, if (matches) selector.nested.get else selector)) {
                elements(index) = Selector.changed
                true
              } else {
                elements.remove(index + 1, elements.size - index)
                false
              }
            }
          case node : Node =>  
            false
          case children =>
            var changed = false
            children foreach { child =>
              if (findMatchedElements(child, selector))
                changed = true
            }
            changed
        }
      }
  
      // Find elements to be replaced
      if (!findMatchedElements(xml, selector)) {
        
        // wait for all futures
        Future.sequence(elements.filter(elem => (elem ne Selector.changed) && (elem ne Selector.unchanged)).map(child.renderAsync(context, _))).map {
          replacements =>
            
            var elementIndex = 0
            var replacementIndex = 0
    
            def build(xml : NodeSeq) : NodeSeq = {
              xml match {
                case elem : Elem =>
                  val elementAtIndex = elements(elementIndex)
                  elementIndex += 1
                  if (elementAtIndex eq elem) {
                    try replacements(replacementIndex)
                    finally replacementIndex += 1
                  } else if (elementAtIndex eq Selector.changed) {
                    elem.copy(child = build(elem.child))
                  } else if (elementAtIndex eq Selector.unchanged) {
                    elem
                  } else {
                    throw new IllegalStateException
                  }
                case node : Node =>
                  node
                case children =>
                  children.flatMap(build)
              }
            }
            
            build(xml)
        }
      }
      
      // No change? Return input.
      else Future.successful(xml)
    } finally {
      elements.clear
    }
  }
}