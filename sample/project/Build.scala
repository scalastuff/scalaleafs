
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

  val scalaleafs = Project(id = "scalaleafs", base = file("../../scalaleafs"))
    
  val sample = Project(id = "scalaleafs-sample", base = file("."), settings = defaultSettings ++ Seq(
    mainClass in (Compile, run) := Some("net.scalaleafs.sample.Main"))).dependsOn(scalaleafs)
}
