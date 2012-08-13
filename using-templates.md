---
layout: default
title: Using Templates
---

This first example shows how to use templates and how to bind them to scala code using [CSS Selectors](/css-selectors.html). The easiest way to use templates is to have an XML file accompanied with a scala class with the same name.

{% highlight html %}
<div>
  <span id="elt1">some text</span>
  <span class="title">some text</span>
  <span class="row1">some text</span>
</div>
{% endhighlight %}
<label>src/main/resources/com/mycom/Sample1.html</label>

{% highlight scala %}
package com.mycom
import net.scalaleafs._

class Sample1 extends Template {
  val bind = 
    "#elt1" #> "Hi there" &
    ".title" #> <h3>Hi There!</h3> &
    "span.row1" #> addClass("selected")
}
{% endhighlight %}
<label>src/main/scala/com/mycom/Sample1.scala</label>

When this template is rendered (`new Sample1().render`), the following output is produced:

{% highlight html %}
<div>
  Hi there
  <h3>Hi There!</h3>
  <span id="elt3" class="selected">some text</span>
</div>
{% endhighlight %}

The example shows the 3 things that can be on the right side of a CSS selector: a string, an XML literal, or an XML transformation. An XML transformation is a NodeSeq => NodeSeq function. Since nearly everything is an XML transformation, things compose nicely. In the example above, the following XML transformations can be identified:

- The expression `"#elt1" #> "Hi there"`
- The expression `"#elt1" #> "Hi there" & "#elt2" #> <h3>Hi There!</h3>`
- The `addClass` function
- The `bind` function
- The `Sample1` class

TODO: explain templates without an accompanying scala class.

## Using MyPage

To use the `MyPage` template, change the MyApp class into:

{% highlight scala %}
class MyApp extends LeafsServlet {
  def render(trail : UrlTrail) = new Sample1().render
}
{% endhighlight %}
