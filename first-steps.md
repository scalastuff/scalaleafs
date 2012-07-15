---
layout: default
title: First Example
---
# First example

The first example shows how to use *CSS Selectors* to bind a template to scala code. The template file should be placed on the classpath.

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

Now, when the temlate is rendered (new FirstExample.render), the following output is produced:

{% highlight html %}
<div>
  Hi there
  <h3>Hi There!</h3>
  <span id="elt3" class="selected">some text</span>
</div>
{% endhighlight %}
