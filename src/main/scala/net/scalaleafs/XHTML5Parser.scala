package net.scalaleafs

import java.io.InputStream
import java.nio.charset.Charset
import scala.xml._
import scala.io.Source
import scala.xml.Source._
import scala.xml.parsing.NoBindingFactoryAdapter
import scala.xml.NamespaceBinding

object XHTML5Parser {

  private val voidElements = Set("area", "base", "br", "col", "command", "embed", "hr", "img", "input", "keygen", "link", "meta", "param", "source", "track", "wbr")
  private val entities = Map(
    "&nbsp;" -> "&#160;",
    "&iexcl;" -> "&#161;",
    "&cent;" -> "&#162;",
    "&pound;" -> "&#163;",
    "&curren;" -> "&#164;",
    "&yen;" -> "&#165;",
    "&brvbar;" -> "&#166;",
    "&sect;" -> "&#167;",
    "&uml;" -> "&#168;",
    "&copy;" -> "&#169;",
    "&ordf;" -> "&#170;",
    "&laquo;" -> "&#171;",
    "&not;" -> "&#172;",
    "&shy;" -> "&#173;",
    "&reg;" -> "&#174;",
    "&macr;" -> "&#175;",
    "&deg;" -> "&#176;",
    "&plusmn;" -> "&#177;",
    "&sup2;" -> "&#178;",
    "&sup3;" -> "&#179;",
    "&acute;" -> "&#180;",
    "&micro;" -> "&#181;",
    "&para;" -> "&#182;",
    "&middot;" -> "&#183;",
    "&cedil;" -> "&#184;",
    "&sup1;" -> "&#185;",
    "&ordm;" -> "&#186;",
    "&raquo;" -> "&#187;",
    "&frac14;" -> "&#188;",
    "&frac12;" -> "&#189;",
    "&frac34;" -> "&#190;",
    "&iquest;" -> "&#191;",
    "&Agrave;" -> "&#192;",
    "&Aacute;" -> "&#193;",
    "&Acirc;" -> "&#194;",
    "&Atilde;" -> "&#195;",
    "&Auml;" -> "&#196;",
    "&Aring;" -> "&#197;",
    "&AElig;" -> "&#198;",
    "&Ccedil;" -> "&#199;",
    "&Egrave;" -> "&#200;",
    "&Eacute;" -> "&#201;",
    "&Ecirc;" -> "&#202;",
    "&Euml;" -> "&#203;",
    "&Igrave;" -> "&#204;",
    "&Iacute;" -> "&#205;",
    "&Icirc;" -> "&#206;",
    "&Iuml;" -> "&#207;",
    "&ETH;" -> "&#208;",
    "&Ntilde;" -> "&#209;",
    "&Ograve;" -> "&#210;",
    "&Oacute;" -> "&#211;",
    "&Ocirc;" -> "&#212;",
    "&Otilde;" -> "&#213;",
    "&Ouml;" -> "&#214;",
    "&times;" -> "&#215;",
    "&Oslash;" -> "&#216;",
    "&Ugrave;" -> "&#217;",
    "&Uacute;" -> "&#218;",
    "&Ucirc;" -> "&#219;",
    "&Uuml;" -> "&#220;",
    "&Yacute;" -> "&#221;",
    "&THORN;" -> "&#222;",
    "&szlig;" -> "&#223;",
    "&agrave;" -> "&#224;",
    "&aacute;" -> "&#225;",
    "&acirc;" -> "&#226;",
    "&atilde;" -> "&#227;",
    "&auml;" -> "&#228;",
    "&aring;" -> "&#229;",
    "&aelig;" -> "&#230;",
    "&ccedil;" -> "&#231;",
    "&egrave;" -> "&#232;",
    "&eacute;" -> "&#233;",
    "&ecirc;" -> "&#234;",
    "&euml;" -> "&#235;",
    "&igrave;" -> "&#236;",
    "&iacute;" -> "&#237;",
    "&icirc;" -> "&#238;",
    "&iuml;" -> "&#239;",
    "&eth;" -> "&#240;",
    "&ntilde;" -> "&#241;",
    "&ograve;" -> "&#242;",
    "&oacute;" -> "&#243;",
    "&ocirc;" -> "&#244;",
    "&otilde;" -> "&#245;",
    "&ouml;" -> "&#246;",
    "&divide;" -> "&#247;",
    "&oslash;" -> "&#248;",
    "&ugrave;" -> "&#249;",
    "&uacute;" -> "&#250;",
    "&ucirc;" -> "&#251;",
    "&uuml;" -> "&#252;",
    "&yacute;" -> "&#253;",
    "&thorn;" -> "&#254;",
    "&yuml;" -> "&#255;")

  def parse(in: InputStream): Node = {
    val encoding = detectEncoding(in)
    val contents = Source.fromInputStream(in, encoding).mkString
    val contents2 = replaceEntities(contents)
    adapter.loadXML(fromString(contents2), adapter.parser)
  }

  private val adapter = new NoBindingFactoryAdapter {
    override def adapter = this
    override def createNode(pre: String, label: String, attrs: MetaData, scope: NamespaceBinding, children: List[Node]): Elem =
      Elem(pre, label, attrs, scope, voidElements.contains(label), children: _*)
  }

  private def replaceEntities(s : String) : String = {
    val out = new StringBuilder
    var lastIndex = 0
    var index = s.indexOf('&')
    while (index != -1) {
      out.append(s.substring(lastIndex, index))
      val i = s.indexOf(';', index + 1)
      if (i != -1) {
        val entity = s.substring(index, i + 1)
        out.append(entities.getOrElse(entity, "?"))
        index = i + 1
      }
      else {
        out.append('&')
      }
      lastIndex = index
      index = s.indexOf('&', index + 1)
    }
    out.append(s.substring(lastIndex))
    out.toString
  }

  private def detectEncoding(in: InputStream): String = {
    var encoding: String = System.getProperty("file.encoding")
    in.mark(400)
    var ignoreBytes = 0
    var readEncoding = false
    val buffer = new Array[Byte](400)
    var read = in.read(buffer, 0, 4)
    buffer(0) match {
      case 0x00 =>
      if (buffer(1) == 0x00 && buffer(2) == 0xFE && buffer(3) == 0xFF) {
        ignoreBytes = 4
        encoding = "UTF_32BE"
      } else if (buffer(1) == 0x00 && buffer(2) == 0x00
      && buffer(3) == 0x3C) {
        encoding = "UTF_32BE"
        readEncoding = true
      } else if (buffer(1) == 0x3C && buffer(2) == 0x00
      && buffer(3) == 0x3F) {
        encoding = "UnicodeBigUnmarked"
        readEncoding = true
      }
      case 0xFF =>
      if (buffer(1) == 0xFE && buffer(2) == 0x00
      && buffer(3) == 0x00) {
        ignoreBytes = 4
        encoding = "UTF_32LE"
      } else if (buffer(1) == 0xFE) {
        ignoreBytes = 2
        encoding = "UnicodeLittleUnmarked";
      }

      case 0x3C=>
        readEncoding = true;
      if (buffer(1) == 0x00 && buffer(2) == 0x00
      && buffer(3) == 0x00) {
        encoding = "UTF_32LE"
      } else if (buffer(1) == 0x00 && buffer(2) == 0x3F
      && buffer(3) == 0x00) {
        encoding = "UnicodeLittleUnmarked"
      } else if (buffer(1) == 0x3F && buffer(2) == 0x78
      && buffer(3) == 0x6D) {
        encoding = "ASCII"
      }
      case 0xFE =>
      if (buffer(1) == 0xFF) {
        encoding = "UnicodeBigUnmarked"
        ignoreBytes = 2
      }
      case 0xEF =>
      if (buffer(1) == 0xBB && buffer(2) == 0xBF) {
        encoding = "UTF8"
        ignoreBytes = 3
      }
      case 0x4C =>
      if (buffer(1) == 0x6F && buffer(2) == 0xA7
      && buffer(3) == 0x94) {
        encoding = "CP037"
      }
    }
    if (readEncoding) {
      read = in.read(buffer, 4, buffer.length - 4)
      val cs = Charset.forName(encoding)
      val s = new String(buffer, 4, read, cs)
      val pos = s.indexOf("encoding")
      if (pos != -1) {
        var delim : Char = ' '
        var start = s.indexOf(delim = '\'', pos)
        if (start == -1)
          start = s.indexOf(delim = '"', pos)
        if (start != -1) {
          val end = s.indexOf(delim, start + 1)
          if (end != -1)
            encoding = s.substring(start + 1, end)
        }
      }
    }

    in.reset()
    while (ignoreBytes > 0) {
      ignoreBytes -= 1
      in.read()
    }
    encoding
  }
}
