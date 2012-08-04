---
title: Running and Debugging
layout: default
---

## ScalaLeafs Debug Mode

To facilitate development, ScalaLeafs can be run in debug mode. Debug mode is activated by setting the system property `leafsDebugMode`. Resource caching will be disabled and extra logging information will be shown, both on the server and in the browser.

The system property can be set on the command-line (`-DleafsDebugMode=true`) or programmatically from within SBT:

{% highlight scala %}
> eval System.setProperty("leafsDebugMode", "true")
{% endhighlight %}

A 'Debug Mode' marker box will be visible on served HTML pages.

## Run from SBT

{% highlight scala %}
> container:start
{% endhighlight %}

This will start the application inside jetty as configured in build.sbt. When debug mode is activated, resource changes will be reflected immediately. Changes to scala source files, however, will not. Fortunately, we can use SBT to monitor project changes and have the project recompiled automatically:

{% highlight scala %}
> ~;container:start; container:reload /
{% endhighlight %}

## Run and debug in Eclipse

There are many options to run the application from within eclipse. An advantage of running from Eclipse is the ability to debug the application. The debug mode flag can be set in the launch configuration that is used to start the application.

A good option is to use [run-jetty-run](http://code.google.com/p/run-jetty-run/). It is fast and easy to use. Like SBT, it can also monitor project changes. However, Leafs debug mode in combination with hot code replacement will go a long way.


## Publish 

When development is done, the final war file can be generated using SBT:

{% highlight scala %}
> package
{% endhighlight %}

