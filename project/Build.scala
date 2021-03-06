
import sbt._
import Keys._
import com.typesafe.sbteclipse.plugin.EclipsePlugin.EclipseKeys
import com.typesafe.sbteclipse.plugin.EclipsePlugin.EclipseCreateSrc

object ScalaLeafsBuild extends Build {

  def defaultSettings =
    Project.defaultSettings ++
      Seq(
        sbtPlugin := false, 
        organization := "net.scalaleafs",
        version := "1.1.0-SNAPSHOT",
        scalaVersion := "2.11.2",
        scalacOptions ++= Seq("-deprecation", "-unchecked", "-encoding", "utf8", "-feature"),
        //scalacOptions ++= Seq("-language:implicitConversions", "-language:postfixOps", "-language:higherKinds", "-language:existentials"),
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
    libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2" withSources(),
    libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.2" withSources(),
    libraryDependencies +=  "org.clapper" %% "grizzled-slf4j" % "1.0.2" withSources(),
    libraryDependencies +=  "nu.validator.htmlparser" % "htmlparser" % "1.4",
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.6" withSources(),
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test" withSources(),
    libraryDependencies <++= scalaBinaryVersion {
      case "2.11" => Seq(
        "io.spray" %% "spray-json" % "1.2.6" withSources(),
        "io.spray" %% "spray-can" % "1.3.1" withSources(),
        "io.spray" %% "spray-http" % "1.3.1" withSources(),
        "io.spray" %% "spray-client" % "1.3.1" withSources(),
        "io.spray" %% "spray-servlet" % "1.3.1" withSources(),
        "io.spray" %% "spray-routing" % "1.3.1" withSources(),
        "io.spray" %% "spray-caching" % "1.3.1" withSources(),
        "io.spray" %% "spray-testkit" % "1.3.1" % "test" withSources())
      case _ => Seq(
        "io.spray" %% "spray-json" % "1.2.6" withSources(),
        "io.spray" % "spray-can" % "1.3.1" withSources(),
        "io.spray" % "spray-http" % "1.3.1" withSources(),
        "io.spray" % "spray-client" % "1.3.1" withSources(),
        "io.spray" % "spray-servlet" % "1.3.1" withSources(),
        "io.spray" % "spray-routing" % "1.3.1" withSources(),
        "io.spray" % "spray-caching" % "1.3.1" withSources(),
        "io.spray" % "spray-testkit" % "1.3.1" % "test" withSources())
    },

    resolvers += "spray repo" at "http://repo.spray.io"))

  val sample = Project(id = "scalaleafs-sample", base = file("sample"), settings = defaultSettings ++ Seq(
    libraryDependencies +=  "ch.qos.logback" % "logback-classic" % "1.0.13" withSources(),
    libraryDependencies += "io.spray" %%  "spray-json" % "1.2.5" withSources(),
    libraryDependencies +=  "joda-time" % "joda-time" % "2.3" withSources(),
    libraryDependencies +=  "org.joda" % "joda-convert" % "1.5" withSources(),
    mainClass in (Compile, run) := Some("net.scalaleafs.sample.Main"))).dependsOn(scalaleafs)
}
