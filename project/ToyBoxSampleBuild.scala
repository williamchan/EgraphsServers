import sbt._
import Keys._
import play.Project._
import play.core.PlayVersion

import BuildHelpers.ModuleProject

object ToyBoxSampleBuild extends Build {

  val appName    = "toybox-sample"
  val appVersion = "1.0-SNAPSHOT"
  val baseDir = file(".") / "modules" / appName

  // From squeryl.org/getting-started.html
    
  val appDependencies = Seq()

  val main = play.Project(
    appName, 
    appVersion, 
    appDependencies,
    path = baseDir, 
    mainLang = SCALA
  ).settings(
    // Copied from build helpers
    resolvers ++= Seq(
      "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
    ),

    shellPrompt := playPrompt,
    testOptions in Test := Nil,
    commands ++= Seq(playCommand)
  ).dependsOn(ToyBoxBuild.main)

}