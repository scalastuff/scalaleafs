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

(or, alternatively use maven)
### Steps required:
1. Add scalaleafs dependency.
2. Configure web container.
3. IDE integration.

### File: *project-dir*/build.sbt
```
organization := "com.mycom"

name := "myapp"

version := "1.0"

scalaVersion := "2.9.2"

libraryDependencies += "net.scalaleafs" %% "scalaleafs" % "0.1"

libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"

seq(webSettings :_*)
```

### File: *project-dir*/project/plugins.sbt
```
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.0-RC1")

libraryDependencies <+= sbtVersion(v => v match {
case "0.11.0" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.0-0.2.8"
case "0.11.1" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.1-0.2.10"
case "0.11.2" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.2-0.2.11"
case "0.11.3" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.3-0.2.11.1"
})
```

- Add a dependency to scala leafs

```
"net.scalaleafs" %% "scalaleafs" % "0.1"
```

- Create a filter in web.xml:
```
<filter>
    <filter-name>MyApp</filter-name>
    <filter-class>com.mycom.MyApp</filter-class>
</filter>
```

- Create a filter

Let's start by creating a minimal application 'com.mycom.MyApp' using SBT.

## Create SBT file

A minimal build.sbt file looks like:

Create a simple project with the following files:

    src/main/scala/com/mycom/MyApp.scala
    src/main/webapp/WEB-INF/web.xml
    build.sbt

Setup project with sbt / maven

/ My first app / servlet