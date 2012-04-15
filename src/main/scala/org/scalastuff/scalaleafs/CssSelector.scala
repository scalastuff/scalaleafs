package org.scalastuff.scalaleafs

import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable.Builder
import scala.util.parsing.combinator.RegexParsers
import scala.xml.Elem
import scala.xml.Node
import scala.xml.NodeSeq

class CssSelector(val matches : Elem => Boolean, val nested : Option[CssSelector] = None) 
case class TypeSelector(t : String) extends CssSelector(elem => elem.label == t)
case class IdSelector(id : String) extends CssSelector(elem => XmlHelpers.attr(elem, "id") == id)
case class ClassSelector(cl : String) extends CssSelector(elem => XmlHelpers.hasClass(elem, cl))
case class PseudoSelector(id : String) extends CssSelector(_ => false)
case class CompoundSelector(sel : List[CssSelector]) extends CssSelector(elem => (false /: sel)(_ && _.matches(elem)))
case class AttrExists(name : String) extends CssSelector(elem => XmlHelpers.attrExists(elem, name))
case class AttrEq(name : String, value : String) extends CssSelector(elem => XmlHelpers.attr(elem, name) == value)
case class AttrIn(name : String, value : String) extends CssSelector(elem => XmlHelpers.hasAttrValue(elem, name, value))
case class AttrUntilHyphen(name : String, value : String) extends CssSelector(elem => XmlHelpers.attrEqualsUntilHyphen(elem, name, value))
case class AttrStartsWith(name : String, substring : String) extends CssSelector(elem => XmlHelpers.attr(elem, name).startsWith(substring))
case class AttrEndsWith(name : String, substring : String) extends CssSelector(elem => XmlHelpers.attr(elem, name).endsWith(substring))
case class AttrSubstring(name : String, substring : String) extends CssSelector(elem => XmlHelpers.attr(elem, name).indexOf(substring) != -1)

class CssTransformation(cssSel : CssSelector, f : Elem => NodeSeq) extends XmlTransformation {

  def apply(xml : NodeSeq) : NodeSeq = apply(cssSel, xml)

  def apply(cssSel : CssSelector, xml : NodeSeq) : NodeSeq = {
    var changed = false
    var builder = NodeSeq.newBuilder
    xml foreach {
      case elem : Elem if cssSel.matches(elem) => 
        cssSel.nested match {
          case Some(nested) =>
            val recursed = apply(nested, elem.child)
            // Optimization: Don't clone when child list is the same instance
            if (!(recursed eq elem.child)) {
              changed = true;
              builder += elem.copy(child = recursed)
            } else {
              builder += elem
            }
          case None =>
            val replacement = f(elem)
            builder ++= replacement
            changed = true
        }
      case elem : Elem => 
        val recursed = apply(cssSel, elem.child)
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

object UnparsedCssSelector {
  private val cssSelCache = new ConcurrentHashMap[String, CssSelector]
  def parse(s : String) : CssSelector = {
    var cssSel = cssSelCache.get(s)
    if (cssSel == null) {
      CssSelectorParser.parseAll(CssSelectorParser.selectors, s) match {
        case CssSelectorParser.Success(sel, _) =>
          cssSel = sel
          cssSelCache.put(s, cssSel)
        case CssSelectorParser.Failure(msg, _) => 
          throw new Exception("Invalid css selector '" + s + "': " + msg)
        case CssSelectorParser.Error(msg, _) => 
          throw new Exception("Invalid css selector '" + s + "': " + msg)
      } 
    }
    cssSel
  }
}

class UnparsedCssSelector(s : String) {
  def #> (xml : NodeSeq) = new CssTransformation(UnparsedCssSelector.parse(s), _ => xml) 
  def #> (f : Elem => NodeSeq) = new CssTransformation(UnparsedCssSelector.parse(s), f) 
  def #> (f : CssTransformation) = new CssTransformation(UnparsedCssSelector.parse(s), f) 
}

object CssSelectorParser extends RegexParsers {
  override def skipWhitespace = false
  val ID = """[a-zA-Z](-|[a-zA-Z0-9]|_[a-zA-Z0-9])*""".r
  def value = quoted | dquoted | plain
  def plain = """([a-zA-Z0-9]|_[a-zA-Z0-9])*""".r
  def quoted = "'" ~> """(\"|-|[a-zA-Z0-9]|_[a-zA-Z0-9])*""".r <~ "'"
  def dquoted = "\"" ~> """('|-|[a-zA-Z0-9]|_[a-zA-Z0-9])*""".r <~ "\""
  def selectors = repsep(compoundSelector, " ") ^^ (nested(_))
  def compoundSelector = rep(selector) ^^ (s => if (s.size == 1) s(0) else CompoundSelector(s)) 
  def selector = typeSelector | idSelector | classSelector | pseudoSelector | attrSelector 
  def typeSelector = ID ^^ (id => TypeSelector(id))
  def idSelector = "#" ~> ID ^^ (id => IdSelector(id))
  def classSelector = "." ~> ID ^^ (id => ClassSelector(id))
  def pseudoSelector = ":" ~> ID ^^ (id => PseudoSelector(id))
  def attrSelector = "[" ~> attr <~ "]" 
  def attr = attrEq | attrIn | attrUntilHyphen | attrStartsWith | attrEndsWith | attrSubstring | attrExists
  def attrEq = (ID <~ "=") ~ value ^^ (s => AttrEq(s._1, s._2))
  def attrIn = (ID <~ "~=") ~ value ^^ (s => AttrIn(s._1, s._2))
  def attrUntilHyphen = (ID <~ "|=") ~ value ^^ (s => AttrUntilHyphen(s._1, s._2))
  def attrStartsWith = (ID <~ "^=") ~ value ^^ (s => AttrStartsWith(s._1, s._2))
  def attrEndsWith = (ID <~ "$=") ~ value ^^ (s => AttrEndsWith(s._1, s._2))
  def attrSubstring = (ID <~ "*=") ~ value ^^ (s => AttrSubstring(s._1, s._2))
  def attrExists = ID ^^ (s => AttrExists(s))
  def nested(s : List[CssSelector]) : CssSelector = s match {
    case Nil => throw new Exception("cannot happen")
    case s :: Nil => s
    case s :: rest => new CssSelector(s.matches, Some(nested(rest)))
  }
}
