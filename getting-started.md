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
{% highlight scala linenos %}
organization := "com.mycom"

name := "myapp"

version := "1.0"

scalaVersion := "2.9.2"

libraryDependencies += "net.scalaleafs" %% "scalaleafs" % "0.1"

libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"

seq(webSettings :_*)
{% endhighlight %}



