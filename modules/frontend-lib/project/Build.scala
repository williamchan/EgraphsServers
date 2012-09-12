import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "frontend-lib"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "org.joda" % "joda-money" % "0.6"
    )

    val main = PlayProject(appName, appVersion, appDependencies).settings(
      organization := "egraphs"
    )

}
