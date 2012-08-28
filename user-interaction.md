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
  <table id="users">
    <tr>
      <td><span class="first-name"/></td>
      <td><span class="last-name"/></td>
      <td><a class="image">profile</a></td>
    </tr>
  </table>
</html>
{% endhighlight %}
<label>src/main/resources/com/mycom/Sample3.html</label>

{% highlight scala %}
package com.mycom
import net.scalaleafs._

case class User(firstName : String, lastName : String, image : String)

class Sample3(users : List[User]) extends Template {
  val search : Var[String] = Var("")
    
  val visibleUsers : SeqVar[User] = 
    search.mapSeq(s => users.filter(_.firstName.constains(s)))

  val bind = 
    "#search" #> search.bind { s => 
      setAttr("value", s) &
      onchange(s => search.set(s))
    } &
    "#clear-search" #> onclick(search.set("")) &
    "#users tr" #> visibleUsers.bind(_ => <h3>No results</h3>) { user =>
      ".first-name" #> user.firstName &
      ".last-name" #> user.lastName &
      "img" #> setAttr("src", user.img)
    } 
}
{% endhighlight %}
<label>src/main/scala/com/mycom/Sample3.scala</label>
