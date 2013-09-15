---
title: Data Binding
layout: default
published: true
---

This example shows how to use templates and css selectors to serve up some data. Specifically, it shows how to iterate over collections of data.

{% highlight html %}
<html>
  <table id="albums">
    <tr>
      <td><img class="image"/></td>
      <td><span class="title"/></td>
      <td><span class="artist"/></td>
    </tr>
  </table>
</html>
{% endhighlight %}
<label>src/main/resources/com/mycom/Sample2.html</label>

{% highlight scala %}
package com.mycom
import net.scalaleafs._

case class Album(title : String, artist : String, image : String)

class Sample2 extends Template {

  def fetchAlbums = 
    Album("Songs of Love & Hate", "Leonard Cohen", "http://ecx.images-amazon.com/images/I/51mvXVc%2BbqL._AA115_.jpg") :: 
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") :: Nil

  def bind = 
    "#albums tr" #> fetchAlbums.map { album =>
      ".image" #> setAttr("src", album.image) &
      ".title" #> album.title &
      ".artist" #> album.artist 
    } 
}
{% endhighlight %}
<label>src/main/scala/com/mycom/Sample2.scala</label>

The expression `users.map {...}` results in a list of XML transformations, which is
implicitly converted to a transformation that concatenates the results of the individual transformations. The end-result is a natural way to loop over data.

The `setAttr` function is one of the predefined XML transformations in [Xml](http://scalaleafs.net/api/index.html#net.scalaleafs.Xml$).