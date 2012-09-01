---
title: Importing Scala Leafs
layout: default
---

The easiest way to import scala leafs in your source code is by importing everything using a wildcard. This is the approach taken by the examples in this documentation.

{% highlight scala %}
import net.scalaleafs._
{% endhighlight %}

However, the recommended way is to import each class explicitly, and to import the implicit definitions using wildcards:

{% highlight scala %}
import net.scalaleafs.Template
import net.scalaleafs.Html
import net.scalaleafs.implicits._
{% endhighlight %}
