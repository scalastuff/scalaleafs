---
title: Running and Debugging
layout: default
---

## ScalaLeafs Debug Mode

To help development, ScalaLeafs can be run in debug mode. In debug mode, resource caching is disabled and extra debugging information will be logged, both on the server and in the browser.

Debug mode is activated by setting the system property `leafsDebugMode` (to any value). It can be set on the command-line (`-DleafsDebugMode=true`) or programmatically from within SBT:

{% highlight scala %}
> eval System.setProperty("leafsDebugMode", "true")
{% endhighlight %}

A 'Debug Mode' marker box will be visible on served HTML pages.

## Run from SBT

{% highlight scala %}
> container:start
{% endhighlight %}

This will start the application inside jetty as configured in build.sbt. When debug mode is activated, resource changes will be reflected immediately. Changes to scala source files, however, will not. Fortunately, we can use SBT to monitor file changes and have the project recompiled automatically:

{% highlight scala %}
> ~;container:start; container:reload /
{% endhighlight %}

See [xsbt-web-plugin](https://github.com/siasia/xsbt-web-plugin/wiki) for more options.

## Run and debug in Eclipse

There are many options to run the application from within eclipse. An advantage of running from Eclipse is the ability to debug the application. The debug mode flag can be set in the launch configuration that is used to start the application.

A good option is to use [run-jetty-run](http://code.google.com/p/run-jetty-run/). It is fast and easy to use. Like SBT, it can also monitor project changes. However, Leafs debug mode in combination with hot code-replacement will go a long way.

Remember to generate eclipse project files as described in [Setup SBT](/setup-sbt.html).

## Publish 

The final war file can be generated using SBT:

{% highlight scala %}
> package
{% endhighlight %}

