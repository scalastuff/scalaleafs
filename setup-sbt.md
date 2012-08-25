---
layout: default
title: Setup SBT
---

Adding the scalaleafs dependency is essentially all that is required. Releases of scala leafs are in maven-central, cross-built against 2.8.0, 2.8.1, 2.9.0, 2.9.1 and 2.9.2.

{% highlight scala %}
"net.scalaleafs" %% "scalaleafs" % "0.1"
{% endhighlight %}

Typically, however, one wants IDE integration, run the application from SBT or use snapshot releases. This sample SBT configuration will get you going:

{% highlight scala %}
organization := "com.mycom"

name := "myapp"

version := "1.0"

scalaVersion := "2.9.2"

// Add the scalaleafs dependency.
libraryDependencies += "net.scalaleafs" %% "scalaleafs" % "0.1" withSources()

// Use the SBT web plugin.
seq(webSettings :_*)

// Put resource folders on Eclipse build path.
EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

// Use jetty to run the application from SBT.
libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"

// Needed when using scalaleafs snapshot versions. 
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
{% endhighlight %}
<label>build.sbt</label>

{% highlight scala %}
// Enables generation of Eclipse project files.
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.0")

// Add web-plugin to run the application from SBT.
libraryDependencies <+= sbtVersion(v => "com.github.siasia" %% "xsbt-web-plugin" % (v+"-0.2.11"))
{% endhighlight %}
<label>project/plugins.sbt</label>

## Eclipse configuration

To generate or update eclipse project files, run eclipse on the SBT command-line. This will add dependencies and set source folders based on the SBT project definition.

{% highlight scala %}
> eclipse
{% endhighlight %}
