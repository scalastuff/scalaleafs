---
title: User Interaction
layout: default
---

This example shows how to bind a list of data elements to a template.

{% highlight html %}
<table id="users">
  <tr>
    <td><span class="first-name"/></td>
    <td><span class="last-name"/></td>
    <td><a class="profile">profile</a></td>
  </tr>
</table>
{% endhighlight %}
<label>src/main/resources/com/mycom/Sample2.html</label>

{% highlight scala %}
package com.mycom
import net.scalaleafs._

case class User(firstName : String, lastName : String, profilePage : String)

class Sample2(users : List[User]) extends Template {
  val bind = 
    "#users tr" #> users.map { user =>
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
