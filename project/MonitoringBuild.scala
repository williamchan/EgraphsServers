import cloudbees.Plugin._
import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName = "monitoring"
  val appVersion = "1.0-SNAPSHOT"
  val baseDir = file(".") / "apps" / appName

  val appDependencies = Seq(
      
    "com.amazonaws" % "aws-java-sdk" % "1.3.22",
    "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
    "redis.clients" % "jedis" % "2.0.0",
    
    "org.mockito" % "mockito-all" % "1.9.0" % "test",
    "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
    "com.typesafe.akka" % "akka-testkit" % "2.0.2" % "test"
  )

  val main = play.Project(
    appName, 
    appVersion, 
    appDependencies, 
    path = baseDir
  ).settings(
    // Add your own project settings here 
    testOptions in Test := Nil

  ).settings(
    cloudBeesSettings :_*

  ).settings(
    CloudBees.applicationId := Some("monitoring")

  ).dependsOn(
    ToyBoxBuild.main

  )
}
