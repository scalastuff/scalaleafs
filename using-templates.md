---
layout: default
title: Using Templates
---

This first example shows how to use templates and how to bind them to scala code using css selectors. The easiest way to use templates is to have an XML file accompanied with a scala class with the same name. A more detailed explanation can be found in the [Templates](/templates.html) documentation.

{% highlight html %}
<div>
  <span id="title">some text</span>
  <span class="empty">some text</span>
  <span class="row1">some text</span>
</div>
{% endhighlight %}
<label>src/main/resources/com/mycom/Sample1.html</label>

{% highlight scala %}
package com.mycom
import net.scalaleafs._

class Sample1 extends Template {
  val bind = 
    "#title" #> <h3>Hi There!</h3> &
    ".empty" #> "Nothing here" &
    "span.row1" #> addClass("selected")
}
{% endhighlight %}
<label>src/main/scala/com/mycom/Sample1.scala</label>

The template class should inherit from [Template](/api/index.html#net.scalaleafs.Template) and override `bind`.
When this template is rendered (`new Sample1().render`), the following output is produced:

{% highlight html %}
<div>
  <h3>Hi There!</h3>
  Nothing here
  <span id="row1" class="selected">some text</span>
</div>
{% endhighlight %}

The example shows the 3 things that can be on the right side of a CSS selector: an XML literal, a string or an XML transformation. An XML transformation is a NodeSeq => NodeSeq function. Since nearly everything is an XML transformation, things compose nicely. In the example above, the following XML transformations can be identified:

- The expression `".empty" #> "Nothing here"`
- The expression ` ".empty" #> "Nothing here" & "#title" #> <h3>Hi There!</h3>`
- The `addClass` function result
- The `bind` function result
- The `Sample1` class

See [CSS Selectors](/css-selectors.html) for a complete description of allowed selectors.

## Using the template

To use the `Sample1` template, change the MyApp class into:

{% highlight scala %}
class MyApp extends LeafsServlet {
  def render(trail : UrlTrail) = new Sample1().render
}
{% endhighlight %}
