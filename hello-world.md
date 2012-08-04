---
title: Hello World
layout: default
---

The application's starting point is either a servlet or a filter. Define the application class by extending [LeafsServlet](api/index.html#net.scalaleafs.LeafsServlet) or [LeafsFilter](/api/index.html#net.scalaleafs.LeafsFilter) and implement the `render` function. See [servlets](/servlets.html) for more information.

{% highlight scala %}
package com.mycom
import net.scalaleafs._

class MyApp extends LeafsServlet {
  def render(trail : UrlTrail) = <h1>Hello world!</h1>
}
{% endhighlight %}
<label>File: src/main/scala/com/mycom/MyApp.scala</label>

Make sure to register the servlet or filter in `web.xml`:

{% highlight xml %}
<?xml version="1.0" encoding="UTF-8"?>
<web-app>
    <servlet>
       <servlet-name>MyApp</servlet-name>
       <servlet-class>com.mycom.MyApp</servlet-class>
    </servlet>

    <servlet-mapping>
        <servlet-name>MyApp</servlet-name>
        <url-pattern>/*</url-pattern>
    </servlet-mapping>
</web-app>
{% endhighlight %}
<label>File: src/main/webapp/WEB-INF/web.xml</label>

Now, the application can be run from the SBT command line. See also [Running and debugging](/running-debugging.html).

{% highlight scala %}
> container:start
{% endhighlight %}
