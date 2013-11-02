package net.scalaleafs2

import scala.concurrent.Future
import scala.xml.NodeSeq
import scala.xml.Elem
import scala.collection.mutable.ArrayBuffer

class PostponedRenderings {
  
  private var postponedRenderings : Seq[Future[(Elem, NodeSeq)]] = Nil
  
  def postponeRender[A](elem : Elem, f : Future[A], render : A => NodeSeq) : Elem = {
    postponedRenderings :+= f.map(a => elem -> render(a))
    elem
  }
  
  def process(implicit context : Context, xml : NodeSeq) : Future[NodeSeq] = {
    Future.successful(postponedRenderings).map { renderings =>
      postponedRenderings = Nil
      
      
    } 
  }
  
  def transform(context : Context, xml : NodeSeq, selector : Selector, replace : (Context, Elem) => NodeSeq) : NodeSeq = {

    val elements = ArrayBuffer[(Elem, Option[RenderNode])]()
    
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