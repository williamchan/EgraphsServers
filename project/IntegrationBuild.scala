import sbt._
import Keys._
import play.Project._

/**
 * Testing out making a simple integration app.
 */
object IntegrationBuild extends Build {

    val appName         = "integration-app"
    val appVersion      = "2.0-SNAPSHOT"

    val appDependencies = Seq(
      // test dependencies
      "org.scalatest" %% "scalatest" % "1.8" % "test"
    )
  
    val main = play.Project(
      appName,
      appVersion,
      appDependencies,
      path = file(".") / "apps" / "integration-app"
    ).settings(
      organization := "egraphs",
      
      //This is because Play 2.0 by default will send some Specs 2 test options not recognized by ScalaTest
      testOptions in Test := Nil
    )
}
