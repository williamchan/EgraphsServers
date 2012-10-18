import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "monitoring"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
        
        "com.amazonaws" % "aws-java-sdk" % "1.3.22",
        
        "org.mockito" % "mockito-all" % "1.9.0" % "test",
        "org.scalatest" %% "scalatest" % "1.8" % "test",
        "com.typesafe.akka" % "akka-testkit" % "2.0.2" % "test"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    	// Add your own project settings here 
        testOptions in Test := Nil
    )

}
