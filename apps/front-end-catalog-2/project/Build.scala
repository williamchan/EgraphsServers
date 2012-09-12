import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "front-end-catalog"
    val appVersion      = "2.0-SNAPSHOT"

    val appDependencies = Seq(
      "egraphs" %% "frontend" % "2.0-SNAPSHOT",
      "egraphs" %% "frontend-lib" % "1.0-SNAPSHOT",
      "net.debasishg" %% "sjson" % "0.17",
      
      // test dependencies
      "org.scalatest" %% "scalatest" % "1.8" % "test"
    )
  
    val main = PlayProject("main", appVersion, appDependencies, mainLang = SCALA).settings(
      organization := "egraphs",
      
      //This is because Play 2.0 by default will send some Specs 2 test options not recognized by ScalaTest
      testOptions in Test := Nil
    )
}
