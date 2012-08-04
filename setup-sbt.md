---
layout: default
title: Setup SBT
---

Adding the scalaleafs dependency is all that's required.

{% highlight scala %}
"net.scalaleafs" %% "scalaleafs" % "0.1"
{% endhighlight %}

Releases of scalaleafs are in maven-central, cross-built against scala versions 2.8.0, 2.8.1, 2.9.0, 2.9.1 and 2.9.2. Typically, however, one wants IDE integration, run the application from SBT or use snapshot releases. This sample SBT configuration will get you going:

<label>File: build.sbt</label>
{% highlight scala %}
organization := "com.mycom"

name := "myapp"

version := "1.0"

scalaVersion := "2.9.2"

// Add the scalaleafs dependency.
libraryDependencies += "net.scalaleafs" %% "scalaleafs" % "0.1"

// Use the SBT web plugin.
seq(webSettings :_*)

// Put resource folders on Eclipse build path.
EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

// Use jetty to run the application from SBT.
libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"

// Needed when using scalaleafs snapshot versions. 
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
{% endhighlight %}

<br/>
<label>File: project/plugins.sbt</label>
{% highlight scala %}
// Enables generation of Eclipse project files.
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.0-RC1")

// Add web-plugin to run the application from SBT.
libraryDependencies <+= sbtVersion(v => v match {
case "0.11.0" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.0-0.2.8"
case "0.11.1" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.1-0.2.10"
case "0.11.2" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.2-0.2.11"
case "0.11.3" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.3-0.2.11.1"
})
{% endhighlight %}

Running the application

Your application can be run from the SBT command line:

{% highlight scala %}
container:start
{% endhighlight %}

SBT can monitor application changes and reload the application automatically:
