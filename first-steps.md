---
layout: default
title: First Example
---
# First example

The first example shows how HTML is bound to scala code. Scala code references HTML using CSS selectors. The template file should be placed on the classpath.

FirstExample.html:

{% highlight html %}
<div>
  <span id="elt1">some text</span>
  <span id="elt2">some text</span>
  <span id="elt3">some text</span>
</div>
{% endhighlight %}

FirstExample.scala:

{% highlight scala %}
import net.scalaleafs._
class FirstExample extends Template {
  def bind = 
    "#elt1" #> Hi there
    "#elt2" #> <h3>Hi There!</h3>
    "#elt3" #> AddClass("selected")
}
{% endhighlight %}