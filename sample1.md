---
layout: default
title: Sample1
---

This first example shows how to use [CSS Selectors](/css-selectors.html) to bind a template to scala code. 

{% highlight html %}
<div>
  <span id="elt1">some text</span>
  <span id="elt2">some text</span>
  <span id="elt3">some text</span>
</div>
{% endhighlight %}
<label>src/main/resources/com/mycom/Sample1.html</label>

{% highlight scala %}
package com.mycom
import net.scalaleafs._

class Sample1 extends Template {
  val bind = 
    "#elt1" #> "Hi there" &
    "#elt2" #> <h3>Hi There!</h3> &
    "#elt3" #> addClass("selected")
}
{% endhighlight %}
<label>src/main/scala/com/mycom/Sample1.scala</label>

When the template is rendered, the following output is produced:

{% highlight html %}
<div>
  Hi there
  <h3>Hi There!</h3>
  <span id="elt3" class="selected">some text</span>
</div>
{% endhighlight %}

The example shows the 3 things that can be on the right side of a CSS selector: a string, an XML literal, or an XML transformation. An XML transformation is a NodeSeq => NodeSeq function. Since nearly everything is an XML transformation, things composes nicely. In the example above, the following XML transformations can be identified:

- The expression `"#elt1" #> "Hi there"`
- The expression `"#elt1" #> "Hi there" & "#elt2" #> <h3>Hi There!</h3>`
- The `addClass` function
- The `bind` function
- The `Sample1` class

## Using MyPage

To use the `MyPage` template, change the MyApp class into:

{% highlight scala %}
class MyApp extends LeafsServlet {
  def render(trail : UrlTrail) = new MyPage().render
}
{% endhighlight %}
