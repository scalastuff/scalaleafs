
import sbt._
import Keys._
import com.typesafe.sbteclipse.plugin.EclipsePlugin.EclipseKeys
import com.typesafe.sbteclipse.plugin.EclipsePlugin.EclipseCreateSrc

object ScalaLeafsBuild extends Build {

  def defaultSettings =
    Project.defaultSettings ++
      Seq(
        sbtPlugin := false, 
        organization := "com.kentivo",
        version := "1.1.0-SNAPSHOT",
        scalaVersion := "2.10.3",
        scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8", "-feature"),
        scalacOptions ++= Seq("-language:implicitConversions", "-language:postfixOps", "-language:reflectiveCall", "-language:higherKinds", "-language:existentials", "-language:reflectiveCalls"),
        EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource,
        EclipseKeys.withSource := true)
          
  def publishSettings = Seq(
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    licenses := Seq("The Apache Software Licence, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    homepage := Some(url("http://scalaleafs.net")),
    publishTo := {
      if (version.value.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
      else
        Some("releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2")},
    pomExtra := 
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

    
  val scalaleafs = Project(id = "scalaleafs", base = file("."), settings = defaultSettings ++ publishSettings ++ Seq(
    libraryDependencies +=  "org.clapper" %% "grizzled-slf4j" % "1.0.1" withSources(),
    libraryDependencies +=  "nu.validator.htmlparser" % "htmlparser" % "1.4",
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.2.3" withSources(),
    libraryDependencies += "io.spray" % "spray-can" % "1.2.0" withSources(),
    libraryDependencies += "io.spray" % "spray-routing" % "1.2.0" withSources(),
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.0" % "test" withSources(),
    resolvers += "spray repo" at "http://repo.spray.io"))

  val sample = Project(id = "scalaleafs-sample", base = file("sample"), settings = defaultSettings ++ Seq(
    libraryDependencies +=  "ch.qos.logback" % "logback-classic" % "1.0.13" withSources(),
    mainClass in (Compile, run) := Some("net.scalaleafs.sample.Main"))).dependsOn(scalaleafs)
}
