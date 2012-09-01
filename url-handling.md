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
      case "toys" :: _ => new ToysPage(trail.advance).render
    }
}

class BooksPage(trail : UrlTrail) extends Template { ... }
class ToysPage(trail : UrlTrail) extends Template { ... }
{% endhighlight %}

<h2>Ajax Page Switching</h2>

Leafs allows server-side page switching with integrated support for the [history API](http://html5demos.com/history). There are several ways to switch pages, using the request object R is one of them:

{% highlight scala %}
R.changeUrl("page2.html")
{% endhighlight %}

Leafs will look for the appropriate URL handler to re-render the smallest possible part of the page. The page is (partially) re-rendered and the browser's URL is changed through the history API.

A URL handler is an object that implements the [UrlTrail](/api/index.html#net.scalaleafs.UrlHandler) trait. The handler should override the `trail` method. Usually, a template is also the URL handler:

{% highlight scala %}
package com.mycom
import net.scalaleafs._

class PageFrame(val trail : Var[UrlTrail]) extends Template with UrlHandler {
  def bind = trail.bind {
    "#content" #> trail.remainder match {
      case "books" :: _ => new BooksPage(trail.advance).render
      case "toys" :: _ => new ToysPage(trail.advance).render
    }
  }
}
{% endhighlight %}

