import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

  val appName = "front-end-catalog"
  val appVersion = "2.0-SNAPSHOT"

  val appDependencies = Seq(
//    "play" %% "scala" % "0.9.1",
    "org.joda" % "joda-money" % "0.6"
  )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    organization := "egraphs"
  )
    /* Opps, this is all the work for website, not frontend, keeping here to not lose work.
  val appDependencies = Seq(
     // Application dependencies. Keep these in alphabetical order.
    "batik" % "batik-rasterizer" % "1.6",
    "batik" % "batik-svggen" % "1.6",
    "com.google.inject" % "guice" % "2.0",
    "com.stripe" % "stripe-java" % "1.0.1",
    "egraphs" % "frontend" % "HEAD",
    "egraphs" % "redis" % "0.3",
    "junit-addons" % "junit-addons" % "1.4",
    "net.debasishg" %% "sjson" % "0.12" exclude("org.scala-lang", "scala-library"),
//        exclude:
//            # Exclude scala because its included with play-scala
//            org.scala-lang % scala-library 2.8.1
    "org.antlr" % "stringtemplate" % "4.0.2",
    "org.apache.axis2" % "axis2" % "1.6.1",
    "org.apache.axis2" % "axis2-transport-http" % "1.6.1",
    "org.apache.axis2" % "axis2-transport-local" % "1.6.1",
    "org.jclouds.api" % "filesystem" % "1.2.1" excludeAll(
        ExclusionRule(organization = "org.clojure")
    ),
//        exclude:
//            org.clojure % *
    "org.jclouds.provider" % "aws-s3" % "1.2.1" excludeAll(
        ExclusionRule(organization = "org.clojure")
    ),
//        exclude:
//            org.clojure % *
    "org.joda" % "joda-money" % "0.6",
    "joda-time" % "joda-time" % "2.1",
    "org.specs2" %% "specs2" % "1.5" excludeAll(
        ExclusionRule(organization = "org.mockito"),
        ExclusionRule(organization = "org.scala-lang")
    ),
//        exclude:
//            org.mockito % *
//            org.scala-lang % scala-library 2.8.1
    "org.squeryl" %% "squeryl" % "0.9.5-2" excludeAll(
    	// Exclude scala library, which is included with play
        ExclusionRule(organization = "org.scala-lang"),
        // Exclude DB-specific libs
        ExclusionRule(organization = "net.sourceforge.jtds"),
        ExclusionRule(organization = "postgresql"),
        ExclusionRule(organization = "org.apache.derby"),
        ExclusionRule(organization = "com.h2database"),
        ExclusionRule(organization = "mysql"),
        // Exclude testing libs
        ExclusionRule(organization = "junit"),
        ExclusionRule(organization = "org.scalatest")
    ),
//        exclude:
//            org.scala-lang % scala-library 2.8.1
//            net.sourceforge.jtds % *
//            postgresql % *
//            org.apache.derby % *
//            com.h2database % *
//            mysql % *
//            # Exclude testing libs
//            junit % *
//            org.scalatest % *
    "play" % "cloudbees" % "0.2.2",
    "play" % "mockito" % "0.1",
    "provided" % "mockito-all" % "1.9.1-SNAPSHOT",
    "provided" %% "scala-guice" % "0.1",
    "provided" % "tritonus_share" % "0.3.6",
    "provided" % "tritonus_remaining" % "0.3.6",
    "provided" % "xyzmo" % "1.0",
    "xml-apis" % "xml-apis-ext" % "1.3.04",
    "xml-apis" % "xml-apis" % "1.3.04",
    "xuggle" % "xuggle-xuggler" % "5.4"
  )

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
    organization := "egraphs"
  )
*/
}
