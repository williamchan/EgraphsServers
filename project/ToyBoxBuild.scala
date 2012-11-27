import sbt._
import Keys._
import PlayProject._
import play.core.PlayVersion

import BuildHelpers.ModuleProject

object ToyBoxBuild extends Build {

  val appName    = "toybox"
  val appVersion = "1.0-SNAPSHOT"
  val baseDir = file(".") / "modules" / appName

  // From squeryl.org/getting-started.html
    
  val appDependencies = Seq(
    "play" %% "play" % PlayVersion.current,    
  
    "org.scalatest" %% "scalatest" % "1.8" % "test",
    "play" %% "play-test" % PlayVersion.current % "test"
  )

  val main = PlayProject(
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
  ).dependsOn(PlayUtilsBuild.main)

}