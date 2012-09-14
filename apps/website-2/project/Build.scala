import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "website"
    val appVersion      = "2.0-SNAPSHOT"

    val appDependencies = Seq(
      "egraphs" %% "frontend" % "2.0-SNAPSHOT",
      "egraphs" %% "frontend-lib" % "1.0-SNAPSHOT",

   // Application dependencies. Keep these in alphabetical order.
      "batik" % "batik-rasterizer" % "1.6",
      "batik" % "batik-svggen" % "1.6",
      "com.google.inject" % "guice" % "2.0",
      "com.stripe" % "stripe-java" % "1.0.1",
  //    "egraphs" % "redis" % "0.3",
      "junit-addons" % "junit-addons" % "1.4",
      "net.debasishg" %% "sjson" % "0.17" exclude("org.scala-lang", "scala-library"),  //from 0.12
  //        exclude:
  //            # Exclude scala because its included with play-scala
  //            org.scala-lang % scala-library 2.8.1
      "org.antlr" % "stringtemplate" % "4.0.2",
      "org.apache.axis2" % "axis2" % "1.6.2" , // from 1.6.1
      "org.apache.axis2" % "axis2-transport-http" % "1.6.2" intransitive(),
      "org.apache.axis2" % "axis2-transport-local" % "1.6.2" intransitive(),
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
      //"play" % "cloudbees" % "0.2.2",
      //play" % "mockito" % "0.1",
      //"com.googlecode.soundlibs" % "tritonus-share" % "0.3.7-1", // from our unmanaged dependency 0.3.6
      "xml-apis" % "xml-apis-ext" % "1.3.04",
      "xml-apis" % "xml-apis" % "1.3.04", // we might be getting pretty stale here
      "xuggle" % "xuggle-xuggler" % "5.4"
    )

    val main = PlayProject(appName, appVersion, appDependencies).settings(
      organization := "egraphs",

      resolvers += Resolver.url("xugglecode", url("http://xuggle.googlecode.com/svn/trunk/repo/share/java/"))(Resolver.mavenStylePatterns)
    )

}
