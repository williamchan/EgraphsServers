import sbt._
import Keys._
import PlayProject._

/**
 * Testing out making a simple integration app.
 */
object FrontendCatalogBuild extends Build {

    val appName         = "integration-app"
    val appVersion      = "2.0-SNAPSHOT"

    val appDependencies = Seq(
      // test dependencies
      "org.scalatest" %% "scalatest" % "1.8" % "test"
    )
  
    val main = PlayProject(
      appName,
      appVersion,
      appDependencies,
      path = file(".") / "apps" / "integration-app",
      mainLang = SCALA
    ).settings(
      organization := "egraphs",
      
      //This is because Play 2.0 by default will send some Specs 2 test options not recognized by ScalaTest
      testOptions in Test := Nil
    )
}