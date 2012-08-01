---
layout: default
title: Getting Started
category: Tutorial
index: 2
---

## Required steps

1. Add scalaleafs dependency.
2. Create a servlet or servlet filter.
3. Implement the render function.

## Setup SBT

1. Add scalaleafs dependency.
2. Configure web container.
3. IDE integration.

### File: *project-dir*/build.sbt
{% highlight scala %}
organization := "com.mycom"

name := "myapp"

version := "1.0"

scalaVersion := "2.9.2"

libraryDependencies += "net.scalaleafs" %% "scalaleafs" % "0.1"

libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"

seq(webSettings :_*)
{% endhighlight %}

### File: *project-dir*/project/plugins.sbt
{% highlight scala %}
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.0-RC1")

libraryDependencies <+= sbtVersion(v => v match {
case "0.11.0" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.0-0.2.8"
case "0.11.1" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.1-0.2.10"
case "0.11.2" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.2-0.2.11"
case "0.11.3" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.3-0.2.11.1"
})
{% endhighlight %}

