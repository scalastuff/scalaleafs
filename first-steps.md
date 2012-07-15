---
layout: default
title: First Example
---
# First example

This first example shows how to use **CSS Selectors** to bind a template to scala code. 

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
class FirstExample extends Template {
  val bind = 
    "#elt1" #> "Hi there" &
    "#elt2" #> <h3>Hi There!</h3> &
    "#elt3" #> AddClass("selected")
}
{% endhighlight %}

The template file should be placed on the classpath.
When the template is rendered (Template.render), the following output is produced:

{% highlight html %}
<div>
  Hi there
  <h3>Hi There!</h3>
  <span id="elt3" class="selected">some text</span>
</div>
{% endhighlight %}

The example shows the 3 things that can be on the right side of a CSS selector: a string, an XML literal, or an XML transformation. An XML transformation is basically a NodeSeq => NodeSeq function. Since nearly everything is an XML transformation (like the expression `"#elt1" #> "Hi there"`, `bind` and the `FirstExample` class), everthing composes nicely. See [Fun With Tranformations](fun-with-transformations.html).

{% for page in site.pages %}
[{{ page.title }}]({{page.url}})
{% endfor %}
  
<div xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd" xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  {% for post in site.posts %}
  <url>
    <loc>{{ site.baseurl }}{{ post.url }}</loc>
    {% if post.lastmod == null %}
    <lastmod>{{ post.date | date_to_xmlschema }}</lastmod>
    {% else %}
    <lastmod>{{ post.lastmod | date_to_xmlschema }}</lastmod>
    {% endif %}
    <changefreq>weekly</changefreq>
    <priority>1.0</priority>
  </url>
  {% endfor %}
  {% for page in site.pages %}
  {% if page.sitemap != null and page.sitemap != empty %}
  {{ site.baseurl }}{{ page.url }} {{ page.title }}
  <url>
    <loc>{{ site.baseurl }}{{ page.url }}</loc>
    <lastmod>{{ page.sitemap.lastmod | date_to_xmlschema }}</lastmod>
    <changefreq>{{ page.sitemap.changefreq }}</changefreq>
    <priority>{{ page.sitemap.priority }}</priority>
  </url>
  {% endif %}
  {% endfor %}
</div>
