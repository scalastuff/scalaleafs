---
layout: default
title: Getting Started
category: Tutorial
index: 2
---

## Getting started

1. Add a dependency to scala leafs

```
"net.scalaleafs" %% "scalaleafs" % "0.1"
```

2. Create a filter in web.xml:
```
    <filter>
        <filter-name>MyApp</filter-name>
        <display-name>My First Scalaleafs Application</display-name>
        <filter-class>com.mycom.MachineXSUI</filter-class>
    </filter>
```
src/main/webapp/WEB-INF/web.xml:


Let's start by creating a minimal application 'com.mycom.MyApp' using SBT.

## Create SBT file

A minimal build.sbt file looks like:

```
organization := "com.mycom"

name := "myapp"

version := "1.0"

scalaVersion := "2.9.2"

libraryDependencies += "net.scalaleafs" %% "scalaleafs" % "0.1"
```

Create a simple project with the following files:

    src/main/scala/com/mycom/MyApp.scala
    src/main/webapp/WEB-INF/web.xml
    build.sbt

Setup project with sbt / maven

/ My first app / servlet