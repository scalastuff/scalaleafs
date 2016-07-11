---
title: User Interaction
layout: default
---

HTML elements can be bound directly to scala closures.

{% highlight scala %}
"#button" #> onclick(println("button clicked"))
{% endhighlight %}

User interaction usually requires updating the output page as well. The use of `Var`s greatly simplifies this. A var is a mutable data container that can be bound to an XML transformation. Whenever the data changes, the transformation is run again and the result is sent back to the browser as a partial page update. Vars can be mapped onto other vars. See [Var](/var.html) for details.

{% highlight html %}
<html>
  <input id="search" type="text"/>
  <input id="clear-search" type="button" value="clear"/>
  <table id="albums">
    <tr>
      <td><img class="image"/></td>
      <td><span class="title"/></td>
      <td><span class="artist"/></td>
    </tr>
  </table>
</html>
{% endhighlight %}
<label>src/main/resources/com/mycom/Sample3.html</label>

{% highlight scala %}
package com.mycom
import net.scalaleafs._

class Sample3 extends Template {
  
  def fetchAlbums = 
    Album("Songs of Love & Hate", "Leonard Cohen", "http://ecx.images-amazon.com/images/I/51mvXVc%2BbqL._AA115_.jpg") :: 
    Album("Are you gonna go my way", "Lenny Kravitz", "http://ecx.images-amazon.com/images/I/51QbegkJVkL._AA115_.jpg") :: Nil

  val search : Var[String] = Var("")
    
  val visibleAlbums : SeqVar[Album] = 
    search.mapSeq(s => fetchAlbums.filter(a => a.title.contains(s) || a.artist.contains(s)))

  val bind = 
    "#search" #> search.bind { s => 
      setAttr("value", s) &
      Html.onchange(s => search.set(s))
    } &
    "#clear-search" #> Html.onclick(search.set("")) &
    "#albums tr" #> visibleAlbums.bind(_ => <h3>No results</h3>) { album =>
      ".image" #> setAttr("src", album.image) &
      ".title" #> album.title &
      ".artist" #> album.artist 
    } 
}
{% endhighlight %}
<label>src/main/scala/com/mycom/Sample3.scala</label>

Two vars are being used. One that contains the current search string, one that contains the albums filtered by the current search string. Since the search string is mapped onto album list, the album list is recalculated whenever the search string is changed.

Both vars are bound. The HTML search box will always correspond to the current search string and the album list is re-rendered whenever `visibleAlbums` changes. A `SeqVar` differs from a normal var in that it binds XML for each element and concatenates the result. A special transformation can be provided (`_ => <h3>No results</h3>`) for the empty collection case.