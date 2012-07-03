package org.scalastuff.scalaleafs

import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable.Builder
import scala.util.parsing.combinator.RegexParsers
import scala.xml.Elem
import scala.xml.Node
import scala.xml.NodeSeq
import scala.xml.Text
import scala.xml.TopScope

class CssTransformation(selector : CssSelector, replaceWith : Elem => NodeSeq) extends XmlTransformation {

  override def apply(xml : NodeSeq) : NodeSeq = apply(selector, replaceWith, xml)

  // TODO replace by XmlTransformation.transform
  def apply(cssSel : CssSelector, replaceWith : Elem => NodeSeq, xml : NodeSeq) : NodeSeq = {
    var changed = false
    var builder = NodeSeq.newBuilder
    xml foreach {
      case elem : Elem if cssSel.matches(elem) => 
        cssSel.nested match {
          case Some(nested) =>
            val recursed = apply(nested, replaceWith, elem.child)
            // Optimization: Don't clone when child list is the same instance
            if (!(recursed eq elem.child)) {
              changed = true;
              builder += elem.copy(child = recursed)
            } else {
              builder += elem
            }
          case None =>
            val replacement = replaceWith(elem)
            builder ++= replacement
            changed = true
        }
      case elem : Elem => 
        val recursed = apply(cssSel, replaceWith, elem.child)
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

object CssSelector {
  private val cssSelCache = new ConcurrentHashMap[String, CssSelector]
  def getOrParse(s : String) : CssSelector = {
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
  def #> (text : String) = new CssTransformation(CssSelector.getOrParse(s), _ => Text(text)) 
  def #> (xml : NodeSeq) = new CssTransformation(CssSelector.getOrParse(s), _ => xml) 
  def #> (f : Elem => NodeSeq) = new CssTransformation(CssSelector.getOrParse(s), f) 
  def #> (f : CssTransformation) = new CssTransformation(CssSelector.getOrParse(s), f) 
  def #> (f : XmlTransformation) = new CssTransformation(CssSelector.getOrParse(s), f) 
}

class CssSelector(val matches : Elem => Boolean, val nested : Option[CssSelector] = None) 
case class TypeSelector(prefix : Option[String], label : String) extends CssSelector(elem => Option(elem.prefix) == prefix && elem.label == label)
case class IdSelector(id : String) extends CssSelector(elem => XmlHelpers.attr(elem, "id") == id)
case class ClassSelector(cl : String) extends CssSelector(elem => XmlHelpers.hasClass(elem, cl))
case class PseudoSelector(id : String) extends CssSelector(_ => false)
case class CompoundSelector(sel : List[CssSelector]) extends CssSelector(elem => (true /: sel)(_ && _.matches(elem)))
case class AttrExists(name : String) extends CssSelector(elem => XmlHelpers.attrExists(elem, name))
case class AttrEq(name : String, value : String) extends CssSelector(elem => XmlHelpers.attr(elem, name) == value)
case class AttrIn(name : String, value : String) extends CssSelector(elem => XmlHelpers.hasAttrValue(elem, name, value))
case class AttrUntilHyphen(name : String, value : String) extends CssSelector(elem => XmlHelpers.attrEqualsUntilHyphen(elem, name, value))
case class AttrStartsWith(name : String, substring : String) extends CssSelector(elem => XmlHelpers.attr(elem, name).startsWith(substring))
case class AttrEndsWith(name : String, substring : String) extends CssSelector(elem => XmlHelpers.attr(elem, name).endsWith(substring))
case class AttrSubstring(name : String, substring : String) extends CssSelector(elem => XmlHelpers.attr(elem, name).indexOf(substring) != -1)

trait AbstractCssParser extends RegexParsers {
  override def skipWhitespace = false
  val ID = """[a-zA-Z](-|[a-zA-Z0-9]|_[a-zA-Z0-9])*""".r
  def value = quoted | dquoted | plain
  def plain = """([a-zA-Z0-9]|-|_[a-zA-Z0-9])*""".r
  def quoted = "'" ~> """(\"|-|[a-zA-Z0-9]|_[a-zA-Z0-9])*""".r <~ "'"
  def dquoted = "\"" ~> """('|-|[a-zA-Z0-9]|_[a-zA-Z0-9])*""".r <~ "\""
}

object CssSelectorParser extends AbstractCssParser {
  def selectors = repsep(compoundSelector, " ") ^^ (nested(_))
  def compoundSelector = rep(selector) ^^ (s => if (s.size == 1) s(0) else CompoundSelector(s)) 
  def selector = typeSelector | idSelector | classSelector | pseudoSelector | attrSelector 
  def typeSelector = opt(ID <~ ":") ~ ID ^^ (id => TypeSelector(id._1, id._2))
  def idSelector = "#" ~> ID ^^ (id => IdSelector(id))
  def classSelector = "." ~> ID ^^ (id => ClassSelector(id))
  def pseudoSelector = ":" ~> ID ^^ ((id : String) => PseudoSelector(id))
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

object CssConstructor {
  private val constructorCache = new ConcurrentHashMap[String, CssConstructor]
  def getOrParse(s : String) : CssConstructor = {
    var constructor = constructorCache.get(s)
    if (constructor == null) {
      CssConstructorParser.parseAll(CssConstructorParser.constructors, s) match {
        case CssConstructorParser.Success(sel, _) =>
          constructor = sel
          constructorCache.put(s, constructor)
        case CssConstructorParser.Failure(msg, _) => 
          throw new Exception("Invalid css constructor '" + s + "': " + msg)
        case CssConstructorParser.Error(msg, _) => 
          throw new Exception("Invalid css constructor '" + s + "': " + msg)
      } 
    }
    constructor
  }
}

trait CssConstructor extends Function1[NodeSeq, Elem]

class ElemConstructor(prefix : Option[String], label : String, modifier : ElemModifier) extends CssConstructor {
  override def apply(xml : NodeSeq) : Elem = {
    modifier(Elem(prefix getOrElse null, label, scala.xml.Null, TopScope, xml:_*))
  }
}

object CssConstructorParser extends AbstractCssParser {
  def constructors = rep1sep(constructor, " ") ^^ (nested(_))
  def constructor = opt(ID <~ ":") ~ ID ~ modifiers ^^ (id => new ElemConstructor(id._1._1, id._1._2, id._2)) 
  def modifiers = rep(modifier) ^^ (_.foldLeft(Ident.asInstanceOf[ElemModifier])(_ & _))
  def modifier = idModifier | classModifier | attrModifier 
  def idModifier = "#" ~> ID ^^ (id => SetAttr("id", id))
  def classModifier = "." ~> ID ^^ (id => SetClass(id))
  def attrModifier = "[" ~> attrAssignment <~ "]" 
  def attrAssignment = (ID <~ "=") ~ value ^^ (s => SetAttr(s._1, s._2))
  def nested(s : List[CssConstructor]) : CssConstructor = s match {
    case Nil => throw new Exception("cannot happen")
    case s :: Nil => s
    case s :: rest => new CssConstructor {
      override def apply(xml : NodeSeq) : scala.xml.Elem = {
        s(rest.foldRight(xml)((a, b) => a(b)))
      }
    }
  }  
}
