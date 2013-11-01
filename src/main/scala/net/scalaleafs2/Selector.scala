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

class SelectorSyncRenderNode(selector : Selector, val child : SyncRenderNode) extends SyncRenderNode with SingleChildRenderNode {

  override def render(context : Context, xml : NodeSeq) = 
    Selector.transform(context, xml, selector, child.render)
    
  override def renderChanges(context : Context) = 
    child.renderChanges(context)
} 

/**
 * Render node that uses a selector to transform xml.
 */
class SelectorRenderNode(selector : Selector, mkchild : => RenderNode) extends RenderNode with MultiChildrenRenderNode {
  
  val elements = ArrayBuffer[(Elem, Option[RenderNode])]()
  val children = ArrayBuffer[RenderNode]()
  
  override def renderAsync(context : Context, xml : NodeSeq) : Future[NodeSeq] = {
    import context.executionContext
    
    elements.clear
    var childIndex = 0
    
    def getOrCreateChild = { 
      if (children.size <= childIndex) 
        children += mkchild
      childIndex += 1
      children(childIndex - 1)
    } 
    
    def findMatchedElements(xml : NodeSeq, selector : Selector) {
      xml match {
        case elem : Elem =>
          val index = elements.size
          val matches = selector.matches(elem) 
          if (matches && selector.nested == None) {
            elements += elem -> Some(getOrCreateChild)
          }
          else {
            elements += elem -> None
            val curChildIndex = childIndex
            findMatchedElements(elem.child, if (matches) selector.nested.get else selector) 
            if (curChildIndex == childIndex) {
              elements.remove(elements.size - 1)
            }
          }
        case node : Node =>  
        case children =>
          children foreach(findMatchedElements(_, selector))
      }
    }

    findMatchedElements(xml, selector)
    
    // Dispose unused children
    for (i <- childIndex until children.size)
      children(i).dispose(context)
    
    
    // Render all child nodes
    val renderAll = Future.sequence(elements.collect {
      case (elem, Some(renderNode)) => renderNode.renderAsync(context, elem)
    })
    renderAll.map { replacements =>
        
        var elementIndex = 0
        var replacementIndex = 0

        def build(xml : NodeSeq) : NodeSeq = {
          xml match {
            case elem : Elem if elementIndex < elements.size =>
              val (e, n) = elements(elementIndex) 
              if (e eq elem) {
                elementIndex += 1
                if (n.isDefined) 
                  try replacements(replacementIndex)
                  finally replacementIndex += 1
                else
                  elem.copy(child = build(elem.child))
              } else 
                elem
            case node : Node =>
              node
            case children =>
              children.flatMap(build)
          }
        }
        
        try build(xml)
        finally elements.clear
    }
  }
  
  def renderChangesAsync(context : Context) =
    RenderNode.renderChangesAsync(context, children)
}