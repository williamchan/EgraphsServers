import sbt._
import Keys._
import play.Project._
import com.typesafe.sbteclipse.core.EclipsePlugin.EclipseKeys
import cloudbees.Plugin._

/**
 * Builds the main Egraphs website.
 *
 * This is available to Play / SBT as the project "website"
 */
object WebsiteBuild extends Build {

    val appName         = "website"
    val appVersion      = "2.0-SNAPSHOT"

    val appDependencies = Seq(
   // Application dependencies. Keep these in alphabetical order.
      "batik" % "batik-rasterizer" % "1.6",
      "batik" % "batik-svggen" % "1.6",
      "com.googlecode.mp4parser" % "isoparser" % "1.0-RC-15", 
      "com.google.zxing" % "core" % "2.0",
      "com.google.zxing" % "javase" % "2.0",
      "com.stripe" % "stripe-java" % "1.0.1",
      "com.typesafe" %% "play-plugins-mailer" % "2.1.0" excludeAll(
        ExclusionRule(organization="com.cedarsoft")
      ),
      "org.antlr" % "stringtemplate" % "4.0.2",
      "org.apache.commons" % "commons-email" % "1.2",
      "org.apache.commons" % "commons-lang3" % "3.1",
      "org.apache.pdfbox" % "pdfbox" % "1.7.1",
      "org.jclouds" % "jclouds-blobstore" % "1.4.2",
      "org.jclouds.api" % "filesystem" % "1.4.2",
      "org.jclouds.provider" % "aws-s3" % "1.4.2",
      "org.joda" % "joda-money" % "0.6",
      "org.specs2" %% "specs2" % "1.13" excludeAll(
          ExclusionRule(organization = "org.mockito"),
          ExclusionRule(organization = "org.scala-lang")
      ),
      "org.twitter4j" % "twitter4j-core" % "3.0.3",
      "org.squeryl" %% "squeryl" % "0.9.5-6" excludeAll(
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
      "com.typesafe.akka" % "akka-agent" % "2.0.2",
      "org.scalaz" %% "scalaz-core" % "6.0.4",
      "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
      "redis.clients" % "jedis" % "2.0.0",
      "xml-apis" % "xml-apis-ext" % "1.3.04",
      "xml-apis" % "xml-apis" % "1.3.04", // we might be getting pretty stale here
      "xuggle" % "xuggle-xuggler" % "5.4",

      // Test dependencies
      "com.typesafe.akka" % "akka-testkit" % "2.0.2" % "test",
      "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
      "org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test"
    )
    
    val websiteBaseDir = file(".") / "apps" / "website"

    val sep = java.io.File.separator
    val timestamp = new java.util.Date().getTime
    val main = play.Project(
      appName,
      appVersion,
      appDependencies,
      path = websiteBaseDir
    ).settings(cloudBeesSettings: _*)
    .settings(
      organization := "egraphs",

      testOptions in Test := Nil,

      resourceDirectory in Test := websiteBaseDir / "test" / "resources",

      unmanagedResourceDirectories in Compile += websiteBaseDir / "resources",

      EclipseKeys.skipParents := false,

      EclipseKeys.classpathTransformerFactories += BuildHelpers.playEclipseClasspathAdditions,

      resolvers ++= Seq(
        "xugglecode" at "http://xuggle.googlecode.com/svn/trunk/repo/share/java",
        "scala-guice" at "https://jenkins-codingwell.rhcloud.com/job/Scala-Guice/lastSuccessfulBuild/artifact/repo"
      ),

      CloudBees.jvmProps := "-Dlogger.resource=prod-logger.xml -Dpidfile.path=/dev/null -DdeploymentTime=" + timestamp + " -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$app_dir -XX:ErrorFile=$app_dir/java_error%p.log",

      CloudBees.deployParams := Map("jvmPermSize" -> "200")
    )
    .dependsOn(FrontendBuild.main, AuthenticityTokenBuild.main, PlayUtilsBuild.main, ToyBoxBuild.main)
}
