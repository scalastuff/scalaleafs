---
title: Hello World
layout: default
---

The application's starting point is either a servlet or a filter. Define the application class by extending LeafsServlet or LeafsFilter and implement the `render` function. See [servlets](/servlets.html) for more information.

<label>File: src/main/scala/com/mycom/MyApp.scala</label>
{% highlight scala %}
package com.mycom
import net.scalaleafs._

class MyApp extends LeafsServlet {
  def render(trail : UrlTrail) = <h1>Hello world!</h1>
}
{% endhighlight %}

Make sure to register the servlet or filter in `web.xml`:

<label>File: src/main/webapp/WEB-INF/web.xml</label>
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

Now, the SBT application can be run from the SBT command line:
```
> container:start
```


