package org.scalastuff.scalaleafs

import scala.xml.NodeSeq
import scala.xml.Elem
import scala.xml.parsing.NoBindingFactoryAdapter
import org.xml.sax.InputSource
import javax.xml.parsers.SAXParser
import scala.xml.Null
import scala.xml.Attribute
import scala.xml.UnprefixedAttribute
import scala.xml.MetaData
import scala.annotation.tailrec

object XmlHelpers {
  
  def attr(elem : Elem, key : String) : String = {
    elem.attributes.get(key) match {
      case Some(nodes) => nodes.text
      case None => ""
    }
  }
    
  def attrExists(elem : Elem, key : String) : Boolean = {
    elem.attributes.get(key) match {
      case Some(nodes) => true
      case None => false
    }
  }
  
  def hasAttrValue(elem : Elem, key : String, value : String) : Boolean = {
    findValue(value, attr(elem, key)) != -1
  }
  
  def attrEqualsUntilHyphen(elem : Elem, key : String, value : String) : Boolean = {
    val attrValue = attr(elem, key)
    attrValue.startsWith(value) && (value.length == attrValue.length || attrValue.charAt(value.length()) == '-')
  }
  
  def setAttr(elem : Elem, key : String, value : String) : Elem = setAttr(elem, key, _ => value)
  
  def removeAttr(elem : Elem, key : String) : Elem = setAttr(elem, key, _ => "")
  
  def addAttrValue(elem : Elem, key : String, value : String) : Elem = setAttr(elem, key, { values =>
    if (findValue(value, values) < 0) values + " " + value
    else values
  })
  
  def removeAttrValue(elem : Elem, key : String, value : String) : Elem = setAttr(elem, key, { values =>
    val index = findValue(value, values)
    if (index < 0) values
    else {
      val before = values.substring(0, index).trim
      val after = values.substring(index + value.length).trim
      before + " " + after
    } 
  })

  def hasClass(elem : Elem, value : String) = hasAttrValue(elem, "class", value)

  def setClass(elem : Elem, value : String) = setAttr(elem, "class", value)
  
  def addClass(elem : Elem, value : String) = addAttrValue(elem, "class", value)
  
  def removeClass(elem : Elem, value : String) = removeAttrValue(elem, "class", value)
  
  def setAttr(elem : Elem, key : String, f : String => String) : Elem = {
    var lastAttr : MetaData = Null
    var attr = elem.attributes
    while (attr != Null && attr.key != key) {
      lastAttr = attr
      attr = attr.next 
    }
    if (attr == Null) {
      val value = f("")
      if (value == "") elem
      else elem.copy(attributes = new UnprefixedAttribute(key, value, elem.attributes))
    } else {
      val oldValue = attr.value.text
      val newValue = f(oldValue)
      if (oldValue == newValue) {
        elem
      } else {
        elem.copy(
          attributes = 
            if (newValue == "") cloneUntil(elem.attributes, attr, attr.next)
            else cloneUntil(elem.attributes, attr, new UnprefixedAttribute(key, newValue, attr.next)))
      }
    }
  }
  
  def escape(text : String) : String = {
      escape(text, new StringBuilder).toString
  }
  
  def escape(text : String, out : StringBuilder) : StringBuilder = {
    val len = text.length
    var pos = 0
    while (pos < len) {
      text.charAt(pos) match {
        case '<' => out.append("\\x3C")
        case '>' => out.append("\\x3E")
        case '&' => out.append("\\x26")
        case '"' => out.append("\\x22")
        case c => if (c >= ' ') out.append(c)
      }
      pos += 1
    }
    out
  }

  private def findValue(value : String, values : String) : Integer = {
    var index = values.indexOf(value, 0)
    while (index >= 0) {
      if ((index == 0 || values.charAt(index - 1) == ' ') &&
      (index + value.length == values.length || values.charAt(index + value.length) == ' '))
        return index;
    }
    -1
  }
 
  private def cloneUntil(attr : MetaData, until : MetaData, next : MetaData) : MetaData = {
    if (attr == until) next
    else attr.copy(cloneUntil(attr.next, until, next))
  }
}

class RichElem(elem : Elem) {
  def attr(key : String) : String = XmlHelpers.attr(elem, key)
  def attrExists(key : String) : Boolean = XmlHelpers.attrExists(elem, key)
  def hasAttrValue(key : String, value : String) : Boolean = XmlHelpers.hasAttrValue(elem, key, value)
  def attrEqualsUntilHyphen(key : String, value : String) : Boolean = XmlHelpers.attrEqualsUntilHyphen(elem, key, value)
  def setAttr(key : String, value : String) : Elem = XmlHelpers.setAttr(elem, key, value)
  def removeAttr(key : String) : Elem = XmlHelpers.removeAttr(elem, key)
  def addAttrValue(key : String, value : String) : Elem = XmlHelpers.addAttrValue(elem, key, value)
  def removeAttrValue(key : String, value : String) : Elem = XmlHelpers.removeAttrValue(elem, key, value)
  def hasClass(value : String) = XmlHelpers.hasClass(elem, value)
  def setClass(value : String) = XmlHelpers.setClass(elem, value)
  def addClass(value : String) = XmlHelpers.addClass(elem, value)
  def removeClass(value : String) = XmlHelpers.removeClass(elem, value)
  def setAttr(key : String, f : String => String) : Elem = XmlHelpers.setAttr(elem, key, f)
}

object ElemWithId {
  def unapply(xml : NodeSeq) : Option[(Elem, String)] = xml match {
    case elem : Elem =>
      val id = XmlHelpers.attr(elem, "id")
      if (id.trim != "") Some(elem, id)
      else None
    case _ => None
  }
}



class HTML5Parser extends NoBindingFactoryAdapter {

  override def loadXML(source : InputSource, _p: SAXParser) = {
    loadXML(source)
  }

  def loadXML(source : InputSource) = {
    import nu.validator.htmlparser.{sax,common}
    import sax.HtmlParser
    import common.XmlViolationPolicy

    val reader = new HtmlParser
    reader.setXmlPolicy(XmlViolationPolicy.ALLOW)
    reader.setContentHandler(this)
    reader.parse(source)
    rootElem
  }
}