---
title: Servlet
layout: default
index: 2
---

#Servlets

An application exposes itself through the Servlet API, either as a servlet or a filter. Inherit a class from either LeafsServlet or LeafsFilter. It should implement the render function and possibly override the configuration field. The application's `web.xml` should point to this class.

{% highlight scala %}
package com.mycom
import net.scalaleafs._

class MyApp extends ScalaFilter {
  def render(trail : UrlTrail) = <h1>Hello world</h1>
}
{% endhighlight %}
<span class="label">src/main/scala/com/mycom/MyApp.scala</span>