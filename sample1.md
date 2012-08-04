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

The example shows the 3 things that can be on the right side of a CSS selector: a string, an XML literal, or an XML transformation. An XML transformation is basically a NodeSeq => NodeSeq function. Since nearly everything is an XML transformation (like the expression `"#elt1" #> "Hi there"`, `bind`, `AddClass` and the `FirstExample` class), everthing composes nicely. See [Fun With Tranformations](fun-with-transformations.html).
