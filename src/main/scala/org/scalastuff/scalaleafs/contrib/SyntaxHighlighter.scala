package org.scalastuff.scalaleafs.contrib
import org.scalastuff.scalaleafs.HeadContribution
import org.scalastuff.scalaleafs.TransientRequest
import org.scalastuff.scalaleafs.ConfigVar
import scala.xml.NodeSeq

object SyntaxHighlighterUrl extends ConfigVar[String]("")

object SyntaxHighlighterHeadContribution extends HeadContribution("syntaxHighlighter") {
  def render(request : TransientRequest) = {
    Seq (
      <script type="text/javascript" src={SyntaxHighlighterUrl} />,
      <link href="css/shCore.css" rel="stylesheet" type="text/css" />,
      <link href="css/shThemeDefault.css" rel="stylesheet" type="text/css" />
    )
  }
}

class SyntaxHighlighterBrushHeadContribution(brush : String) extends HeadContribution("syntaxHighlighter:" + brush) {
  def render(request : TransientRequest) = {
    <script type="text/javascript" src={"css/shBrush" + brush + ".js"}></script>
  }
}

object SyntaxHighlighterJavascriptBrush extends SyntaxHighlighterBrushHeadContribution("JScript")