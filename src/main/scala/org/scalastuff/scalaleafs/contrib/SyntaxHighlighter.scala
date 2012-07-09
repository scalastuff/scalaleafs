package org.scalastuff.scalaleafs.contrib
import org.scalastuff.scalaleafs.HeadContribution
import org.scalastuff.scalaleafs.Request
import org.scalastuff.scalaleafs.ConfigVar
import scala.xml.NodeSeq

object SyntaxHighlighterUrl extends ConfigVar[String]("")

object SyntaxHighlighterHeadContribution extends HeadContribution("syntaxHighlighter") {
  def render(request : Request) = {
    Seq (
      <script type="text/javascript" src={SyntaxHighlighterUrl} />,
      <link href="css/shCore.css" rel="stylesheet" type="text/css" />,
      <link href="css/shThemeDefault.css" rel="stylesheet" type="text/css" />
    )
  }
}

class SyntaxHighlighterBrushHeadContribution(brush : String) extends HeadContribution("syntaxHighlighter:" + brush) {
  def render(request : Request) = {
    <script type="text/javascript" src={"css/shBrush" + brush + ".js"}></script>
  }
}

object SyntaxHighlighterJavascriptBrush extends SyntaxHighlighterBrushHeadContribution("JScript")