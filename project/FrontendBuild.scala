import sbt._
import Keys._
import PlayProject._

/**
 * Builds the front-end Play 2.0 module. Provides the 'frontend' project to Play / SBT.
 */
object FrontendBuild extends Build {

  val appName = "front-end"
  val appVersion = "2.0-SNAPSHOT"

  val appDependencies = Seq(
    "crionics" %% "play2-authenticitytoken" % "1.0-SNAPSHOT",
    "org.joda" % "joda-money" % "0.6"
  )

  // Only compile the bootstrap bootstrap.less file and any other *.less file in the stylesheets directory
  def customLessEntryPoints(base: File): PathFinder = (
    (
        (base / "app" / "assets" / "stylesheets" * "*.less") ---
        (base / "app" / "assets" / "stylesheets" * "_*.less") // exclude incomplete less files
    ) +++
    (base / "bootstrap" / "less" / "bootstrap" * "bootstrap.less") //twitter bootstrap
  )

  val main = PlayProject(
    appName,
    appVersion,
    appDependencies,
    path = file(".") / "modules" / "frontend-2",
    mainLang = SCALA
  ).settings(
    //resolvers += "Crionics Github Repository" at "http://orefalo.github.com/m2repo/releases/",

    organization := "egraphs",

    lessEntryPoints <<= baseDirectory(customLessEntryPoints),

    // exclude anything created by the routes file from going into the jar so it doesn't conflict with the packages
    // that pull this in as a dependency.
    mappings in (Compile,packageBin) ~= { (ms: Seq[(File, String)]) =>
      ms filterNot { case (file, toPath) =>
        toPath == "routes" ||
        toPath == "Routes.class" ||
        toPath == "Routes$.class" ||
        toPath == "Routes$$anonfun$routes$1.class" ||
        toPath == "Routes$$anonfun$routes$1$$anonfun$apply$1.class" ||
        toPath == "Routes$$anonfun$routes$1$$anonfun$apply$1$$anonfun$apply$2.class" ||
        toPath == "controllers/javascript/ReverseAssets.class" ||
        toPath == "controllers/ref/ReverseAssets.class" ||
        toPath == "controllers/ref/ReverseAssets$$anonfun$at$1.class" ||
        toPath == "controllers/ReverseAssets.class" ||
        toPath == "controllers/routes.class" ||
        toPath == "controllers/routes$javascript.class" ||
        toPath == "controllers/routes$ref.class"
      }
    }
  )
}
