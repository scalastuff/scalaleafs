---
layout: default
title: First Example
---
# First example

FirstExample.html:

    <div>
      <span id="elt1"/>
      <span id="elt2"/>
      <span id="elt3"/>
    </div>

FirstExample.scala:
{% highlight scala %}
import net.scalaleafs._
class FirstExample extends Template {
  def bind = 
    "#elt1" {%"#>"%} Hi there
    "#elt2" #> <h3>Hi There!</h3>
    "#elt3" #> AddClass("selected")
}
{% endhighlight %}