---
title: Servlets
layout: default
---

An application exposes itself through the Servlet API, either as a servlet or a filter. Inherit a class from either LeafsServlet or LeafsFilter. It should implement the render function and possibly override the configuration field. The application's `web.xml` should point to this class.

{% highlight scala %}
package com.mycom
import net.scalaleafs._

class MyApp extends ScalaFilter {
  def render(trail : UrlTrail) = <h1>Hello world</h1>
}
{% endhighlight %}
<span class="label">src/main/scala/com/mycom/MyApp.scala</span>

Your application should and does not need any further dependency on the Servlet API. This makes trasition to other types of containers painless. For example, support for Unfiltered is in the works. 