---
layout: default
title: Getting Started
category: Tutorial
index: 2
---

## Setup SBT

We will configure SBT to do the following:

 1. Add scalaleafs dependency.
 2. Configure web container.
 3. IDE integration.

Steps 2 and 3 are not strictly necessary, but are useful during building and testing.
The provided samples use jetty and eclipse, but this is by no means a necessity.

File: *project-dir*/build.sbt
{% highlight scala %}
organization := "com.mycom"

name := "myapp"

version := "1.0"

scalaVersion := "2.9.2"

seq(webSettings :_*)

libraryDependencies += "net.scalaleafs" %% "scalaleafs" % "0.1"

libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
{% endhighlight %}

File: *project-dir*/project/plugins.sbt

{% highlight scala %}
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.0-RC1")

libraryDependencies <+= sbtVersion(v => v match {
case "0.11.0" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.0-0.2.8"
case "0.11.1" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.1-0.2.10"
case "0.11.2" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.2-0.2.11"
case "0.11.3" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.3-0.2.11.1"
})
{% endhighlight %}

