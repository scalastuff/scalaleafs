---
title: Running and Debugging
layout: default
---

## ScalaLeafs Debug Mode

To facilitate development, ScalaLeafs can be run in debug mode. Debug mode is activated by setting the system property `leafsDebugMode` to `"true"`. Resource caching will be disabled and extra logging information will be shown, both on the server and in the browser.

## Run from SBT

{% highlight scala %}
> container:start
{% endhighlight %}

This will start the application inside jetty as configured in build.sbt. However, during development you better turn on debug mode and have SBT monitor source file changes:

{% highlight scala %}
> eval System.setProperty("leafsDebugMode", "true")
> ~;container:start; container:reload /
{% endhighlight %}

## Publish 

When development is done, the final war file can be generated using SBT:

{% highlight scala %}
> package
{% endhighlight %}

