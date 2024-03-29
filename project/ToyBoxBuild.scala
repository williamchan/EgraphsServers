import sbt._
import Keys._
import play.Project._
import play.core.PlayVersion

import BuildHelpers.ModuleProject

object ToyBoxBuild extends Build {

  val appName    = "toybox"
  val appVersion = "1.0-SNAPSHOT"
  val baseDir = file(".") / "modules" / appName

  // From squeryl.org/getting-started.html
    
  val appDependencies = Seq(
    "play" %% "play" % PlayVersion.current,

    "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
    "play" %% "play-test" % PlayVersion.current % "test"
  )

  val main = play.Project(
    appName, 
    appVersion, 
    appDependencies,
    path = baseDir
  ).settings(
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),

    parallelExecution in Test := false,
    Keys.fork in Test := false,
    testOptions in Test := Nil,

    // Copied from build helpers
    resolvers ++= Seq(
      "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
    ),

    shellPrompt := playPrompt,
    commands ++= Seq(playCommand)
  ).dependsOn(PlayUtilsBuild.main)

}