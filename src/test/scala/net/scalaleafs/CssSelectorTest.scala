package net.scalaleafs

import net.scalaleafs.implicits._

class CssSelectorTest extends FlatSpec {

  def test() {
    val input = <div><div id="parent"><div class="red"><h1>Title</h1></div></div></div>
    val selector = "#parent .red h1" #> <h2>Smaller title</h2> &
      "#parent h1" #> <h2>Smaller title</h2>
    val output = selector(input)
    println(output)
  }

  def parserTest() {
    println(CssSelectorParser.parseAll(CssSelectorParser.selectors, "parent.div#id:dfk"))
    val result = CssSelectorParser.parseAll(CssSelectorParser.selectors, "#parent div.red h1")
    println(result)
  }

  def main(args: Array[String]): Unit = {
    val xml = <span class="one two three"/>
      println(xml.attributes)
    println("Starting")
    var line = readLine;
    while (line != "q") {
      println(CssSelectorParser.parseAll(CssSelectorParser.selectors, line))
      line = readLine
    }
  }
}