package net.scalaleafs2

import scala.xml.NodeSeq
import scala.xml.Elem


/**
 * An Selector is used to match elements in an XML structure. When recursive is set, the xml is searched recursively. 
 * Otherwise (the default) only the top-level nodes are searched. When a nested selector is specified, child nodes of
 * a matched element are searched using the nested selector.
 */
class Selector(val matches : Elem => Boolean, val recursive : Boolean = false, val nested : Option[Selector] = None) 

/**
 * Render node that uses a selector to transform xml.
 */
class SelectorRenderNode(selector : Selector, child : RenderNode) {
  override def apply(context : Context, xml : NodeSeq) = 
    Selector.transform(context, xml, selector, child.render)
    
  override def children = 
    Seq(child)
} 

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
  
}