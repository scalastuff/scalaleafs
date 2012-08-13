---
title: Serving Data
layout: default
---


This first example shows how to use templates and how to bind them to scala code using [CSS Selectors](/css-selectors.html). The easiest way to use templates is to have an XML file accompanied with a scala class with the same name. A more detailed explaination can be found in the [Templates](/templates.html) documentation.

{% highlight html %}
<table id="users">
  <tr>
    <td><span class="first-name"/></td>
    <td><span class="last-name"/></td>
    <td><a class="profile">profile</a></td>
  </tr>
</table>
{% endhighlight %}
<label>src/main/resources/com/mycom/Sample1.html</label>

{% highlight scala %}
package com.mycom
import net.scalaleafs._

case class User(firstName : String, lastName : String, profilePage : String)

class Sample2(users : List[User]) extends Template {

  def bind = 
    "#users tr" #> users.map { user =>
      ".first-name" #> user.firstName &
      ".last-name" #> user.lastName &
      "a.profile" #> setAttr("href", user.profilePage)
    } 
}
{% endhighlight %}
<label>src/main/scala/com/mycom/Sample1.scala</label>

When this template is rendered (`new Sample1().render`), the following output is produced:

{% highlight html %}
<div>
  Nothing here
  <h3>Hi There!</h3>
  <span id="row1" class="selected">some text</span>
</div>
{% endhighlight %}

The example shows the 3 things that can be on the right side of a CSS selector: a string, an XML literal, or an XML transformation. An XML transformation is a NodeSeq => NodeSeq function. Since nearly everything is an XML transformation, things compose nicely. In the example above, the following XML transformations can be identified:

- The expression `".empty" #> "Nothing here"`
- The expression ` ".empty" #> "Nothing here" & "#title" #> <h3>Hi There!</h3>`
- The `addClass` function result
- The `bind` function result
- The `Sample1` class

See [CSS Selectors](/css-selectors.html) for a complete description of allowed CSS selectors.

## Using the template

To use the `Sample1` template, change the MyApp class into:

{% highlight scala %}
class MyApp extends LeafsServlet {
  def render(trail : UrlTrail) = new Sample1().render
}
{% endhighlight %}
