---
title: URL Handling
layout: default
---

A typical Leafs application uses a single-page approach. There is a single template where rendering begins. Other templates are included conditionally, based on, for example, the current request URL. This approach is helped by the [UrlTrail](/api/index.html#net.scalaleafs.UrlTrail) class. A URL trail denotes the process of starting at a root point and finding your way to the current URL.

{% highlight scala %}
package com.mycom
import net.scalaleafs._

class PageFrame(trail : UrlTrail) extends Template {
  def bind = 
    "#content" #> trail.remainder match {
      case "books" :: _ => new BooksPage(trail.advance).render
      case "toys" :: _ => new TiysPage(trail.advance).render
    }
{% endhighlight %}
