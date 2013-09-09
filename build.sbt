
organization := "net.scalaleafs"

name := "scalaleafs"

version := "0.2-SNAPSHOT"

crossScalaVersions := Seq("2.8.0", "2.8.1", "2.9.0", "2.9.1", "2.9.2", "2.10.1")

scalaVersion := "2.10.1"

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

//site.settings

//site.includeScaladoc()

//ghpages.settings

//git.remoteRepo := "git@github.com:scalastuff/scalaleafs.git"

libraryDependencies ++= Seq (
  "org.slf4j" % "slf4j-log4j12" % "1.6.4",
  "javax.servlet" % "servlet-api" % "2.5" withSources(),
  "nu.validator.htmlparser" % "htmlparser" % "1.2.1",
  "org.clapper" %% "grizzled-slf4j" % "1.0.1" withSources(),
  "junit" % "junit" % "4.8" % "test")
  
publishMavenStyle := true

publishArtifact in Test := false

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) 
    Some("snapshots" at nexus + "content/repositories/snapshots") 
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

licenses := Seq("The Apache Software Licence, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

homepage := Some(url("http://scalaleafs.net"))

pomIncludeRepository := { _ => false }

pomExtra := (
  <scm>
    <connection>scm:git:git@github.com:scalastuff/scalaleafs.git</connection>
    <url>https://github.com/scalastuff/scalaleafs</url>
  </scm>
  <developers>
    <developer>
      <id>ruudditerwich</id>
      <name>Ruud Diterwich</name>
      <url>http://ruud.diterwich.com</url>
    </developer>
  </developers>)
  