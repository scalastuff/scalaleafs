
organization := "org.scalastuff"

name := "scalaleafs"

version := "1.0"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq (
  "org.slf4j" % "slf4j-log4j12" % "1.6.4",
  "com.google.guava" % "guava" % "10.0.1",
  "net.databinder" %% "unfiltered" % "0.6.1" withSources(),
  "net.databinder" %% "unfiltered-filter" % "0.6.1" withSources(),
  "net.databinder" %% "unfiltered-jetty" % "0.6.1" withSources(),
  "net.databinder" %% "unfiltered-netty" % "0.6.1",
  "javax.servlet" % "servlet-api" % "2.5" withSources(),
  "nu.validator.htmlparser" % "htmlparser" % "1.2.1",
  "log4j" % "log4j" % "1.2.16",
  "junit" % "junit" % "4.8" % "test")
  
resolvers ++= Seq(
  "repo.novus rels" at "http://repo.novus.com/releases/",
  "repo.novus snaps" at "http://repo.novus.com/snapshots/"
)