package org.scalastuff.scalaleafs
import scala.xml.NodeSeq
import scala.xml.Elem


trait XmlTransformation extends Function1[NodeSeq, NodeSeq] {
  def & (t : XmlTransformation) : XmlTransformation = {
    val first = this
    new XmlTransformation {
      def apply(xml : NodeSeq) : NodeSeq = {
         t(first(xml))
      }
    }
  }
}

object Ident extends XmlTransformation {
  def apply(xml : NodeSeq) = xml
}

object SetAttr {
  def apply(key : String, value : String, condition : => Boolean = true)  = new XmlTransformation {
    def apply(xml : NodeSeq) = xml match {
      case elem : Elem if condition => XmlHelpers.setAttr(elem, key, value)
      case node => node 
    }
  }
}

object RemoveAttr {
  def apply(key : String, value : String, condition : => Boolean = true)  = new XmlTransformation {
    def apply(xml : NodeSeq) = xml match {
      case elem : Elem if condition => XmlHelpers.removeAttr(elem, key)
      case node => node 
    }
  }
}

object AddAttrValue {
  def apply(key : String, value : String, condition : => Boolean = true)  = new XmlTransformation {
    def apply(xml : NodeSeq) = xml match {
      case elem : Elem if condition => XmlHelpers.addAttrValue(elem, key, value)
      case node => node 
    }
  }
}

object RemoveAttrValue {
  def apply(key : String, value : String, condition : => Boolean = true)  = new XmlTransformation {
    def apply(xml : NodeSeq) = xml match {
      case elem : Elem if condition => XmlHelpers.removeAttrValue(elem, key, value)
      case node => node 
    }
  }
}

object SetClass {
  def apply(value : String, condition : => Boolean = false) = new XmlTransformation {
    def apply(xml : NodeSeq) = xml match {
      case elem : Elem if condition => XmlHelpers.setClass(elem, value)
      case node => node 
    }
  }
}

object AddClass {
  def apply(value : String, condition : => Boolean = false) = new XmlTransformation {
    def apply(xml : NodeSeq) = xml match {
      case elem : Elem if condition => XmlHelpers.addClass(elem, value)
      case node => node 
    }
  }
}

object RemoveClass {
  def apply(value : String, condition : => Boolean = false) = new XmlTransformation {
    def apply(xml : NodeSeq) = xml match {
      case elem : Elem if condition => XmlHelpers.removeClass(elem, value)
      case node => node 
    }
  }
}