import sbt._
import Keys._
import play.core.PlayVersion

/*object AuthenticityTokenBuild extends Build {
  val main = Project(id="authenticity-token", base=file("modules/authenticity-token")).settings(
    resolvers ++= Seq(
      "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
    ),

    libraryDependencies += "org.scalatest" %% "scalatest" % "1.8" % "test",

    libraryDependencies += "play" % "play_2.9.1" % PlayVersion.current
  ).dependsOn(PlayUtilsBuild.main)
}*/