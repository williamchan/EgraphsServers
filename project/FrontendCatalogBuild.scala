import sbt._
import Keys._
import play.Project._

/**
 * Builds the front-end catalog Play 2.0 application.
 *
 * Provides the 'front-end-catalog' project to Play / SBT.
 */
object FrontendCatalogBuild extends Build {

    val appName         = "front-end-catalog"
    val appVersion      = "2.0-SNAPSHOT"

    val appDependencies = Seq(      
      // test dependencies
      "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"
    )
  
    val main = play.Project(
      "front-end-catalog",
      appVersion,
      appDependencies,
      path = file(".") / "apps" / "front-end-catalog"
    ).settings(
      organization := "egraphs",
      
      //This is because Play 2.0 by default will send some Specs 2 test options not recognized by ScalaTest
      testOptions in Test := Nil
    ).dependsOn(FrontendBuild.main, AuthenticityTokenBuild.main)
}
