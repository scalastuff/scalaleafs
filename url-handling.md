---
title: URL Handling
layout: default
---

A typical scala leafs application starts at a common root, and refines its rendering based on the following element of the requested URL. A UrlTrail represents this process. It starts at the root URL and the application advances it until it reaches the end, which is the requested URL.

{% highlight scala %}
package com.mycom
import net.scalaleafs._

class PageFrame(trail : UrlTrail) extends Template {
  def bind = 
    "#content" #> trail.next match {
      case "
{% endhighlight %}
