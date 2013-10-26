/**
 * Copyright (c) 2012 Ruud Diterwich.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package net.scalaleafs2

import scala.util.parsing.combinator.RegexParsers
import java.util.concurrent.ConcurrentHashMap
import scala.xml.Elem
import scala.xml.Node
import scala.xml.NodeSeq
import scala.xml.Text
import scala.xml.TopScope

/**
 * A css transformation is a transformation based on a css selector. An instance is typically obtained
 * through the #> operator, e.g. <pre>"#mydiv" #> SetClass("selected")</pre> 
class CssTransformation[A <: (Context, NodeSeq) => NodeSeq](val selector : CssSelector, val transformation : (Context, NodeSeq) => NodeSeq) extends RenderNode {

  override def apply(context : Context, xml : NodeSeq) : NodeSeq = 
    Transformation.transform(context, xml, selector, transformation)
}

class TextCssTransformation(selector : CssSelector, val text : String) 
	extends CssTransformation(selector, (_, _) => Text(text))

class XmlCssTransformation(selector : CssSelector, val xml : NodeSeq) 
	extends CssTransformation[(Context, NodeSeq) => NodeSeq](selector, (_, _) => xml)

 */

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

class UnparsedCssSelector(val s : String) extends AnyVal {
  def #> (node : SyncRenderNode) = new SelectorSyncRenderNode(CssSelector.getOrParse(s), node) 
  def #> (node : RenderNode) = new SelectorRenderNode(CssSelector.getOrParse(s), node) 
}

/**
 * A selector that uses a css syntax. 
 */
class CssSelector(matches : Elem => Boolean, nested : Option[CssSelector] = None) extends Selector(matches, true, nested)
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

/**
 * A CSS constructor creates XML elements with an CSS-like syntax. 
 * For example, the following statement wraps some input xml in an input element with the name attribute set, 
 * which in turn is wrapped in a div element that has the 'wrapper' class.
 * <pre>CssConstructor("div.wrapper input[name='descr']")</pre>
 */
trait CssConstructor extends Function[NodeSeq, Elem]

object CssConstructor {
  private val constructorCache = new ConcurrentHashMap[String, CssConstructor]
  implicit def apply(s : String) : CssConstructor = {
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

object CssConstructorParser extends AbstractCssParser {
  def constructors = rep1sep(constructor, " ") ^^ (nested(_))
  def constructor = opt(ID <~ ":") ~ ID ~ modifiers ^^ (id => new CssConstructor {
    def apply(xml : NodeSeq) = 
      id._2(Elem(id._1._1 getOrElse null, id._1._2, scala.xml.Null, TopScope, xml:_*))
  }) 
  def modifiers = rep(modifier) ^^ (_.foldLeft((elem : scala.xml.Elem) => elem)((m1, m2) => (elem : scala.xml.Elem) => m2(m1(elem))))
  def modifier = idModifier | classModifier | attrModifier 
  def idModifier = "#" ~> ID ^^ (id => (elem : scala.xml.Elem) => XmlHelpers.setAttr(elem, "id", id))
  def classModifier = "." ~> ID ^^ (id => (elem : scala.xml.Elem) => XmlHelpers.setClass(elem, id))
  def attrModifier = "[" ~> attrAssignment <~ "]" 
  def attrAssignment = (ID <~ "=") ~ value ^^ (s => (elem : scala.xml.Elem) => XmlHelpers.setAttr(elem, s._1, s._2))
  def nested(s : List[CssConstructor]) : CssConstructor = s match {
    case Nil => throw new Exception("cannot happen")
    case ctor :: Nil => ctor
    case ctor :: rest => new CssConstructor {
      def apply(xml : NodeSeq) = {
        ctor(rest.foldRight(xml)((ctor, xml) => ctor(xml)))
      }
    }
  }
}
