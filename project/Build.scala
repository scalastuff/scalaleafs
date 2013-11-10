
import sbt._
import Keys._
import com.typesafe.sbteclipse.plugin.EclipsePlugin.EclipseKeys
import com.typesafe.sbteclipse.plugin.EclipsePlugin.EclipseCreateSrc

object SampleBuild extends Build {

  def defaultSettings =
    Project.defaultSettings ++
      Seq(
        sbtPlugin := false, 
        organization := "com.kentivo",
        version := "1.0.0-SNAPSHOT",
        scalaVersion := "2.10.3",
        scalacOptions += "-deprecation",
        scalacOptions += "-unchecked",
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

    
  val scalaleafs = Project(id = "scalaleafs", base = file("."), settings = defaultSettings ++ publishSettings)

  val sample = Project(id = "scalaleafs-sample", base = file("sample"), settings = defaultSettings ++ Seq(
    mainClass in (Compile, run) := Some("net.scalaleafs.sample.Main"))).dependsOn(scalaleafs)
}
