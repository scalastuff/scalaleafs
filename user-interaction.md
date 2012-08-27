---
title: User Interaction
layout: default
---

HTML elements can be bound directly to scala closures.

{% highlight scala %}
"#button" #> onclick(println("button clicked"))
{% endhighlight %}



{% highlight html %}
<html>
  <input id="search" type="text"/>
  <input id="clear-search" type="button" value="clear"/>
  <table id="users">
    <tr>
      <td><span class="first-name"/></td>
      <td><span class="last-name"/></td>
      <td><a class="profile">profile</a></td>
    </tr>
  </table>
</html>
{% endhighlight %}
<label>src/main/resources/com/mycom/Sample3.html</label>

{% highlight scala %}
package com.mycom
import net.scalaleafs._

case class User(firstName : String, lastName : String, profilePage : String)

class Sample3(users : List[User]) extends Template {
  val search : Var[String] = 
    Var("")
    
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
      "a.profile" #> setAttr("href", user.profilePage)
    } 
}
{% endhighlight %}
<label>src/main/scala/com/mycom/Sample2.scala</label>

The expression `users.map {...}` results in a list of XML transformations, which is
implicitly converted to a transformation that concatenates the results of the individual transformations. The end-result is a natural way to loop over data.

The `setAttr` function is one of the predefined XML transformations in [Xml](http://scalaleafs.net/api/index.html#net.scalaleafs.Xml$).
