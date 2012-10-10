import sbt._
import Keys._
import play.core.PlayVersion
import PlayProject._

object BuildHelpers {

  val currentPlayDependency = "play" % "play_2.9.1" % PlayVersion.current

  val defaultModuleSettings = Project.defaultSettings ++ Seq(
    resolvers ++= Seq(
      "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/"
    ),
    
    libraryDependencies ++= Seq(
      "play" %% "play" % PlayVersion.current,    
    
      "org.scalatest" %% "scalatest" % "1.8" % "test",
      "play" %% "play-test" % PlayVersion.current % "test"
    ),

    shellPrompt := playPrompt,

    commands ++= Seq(playCommand)
  )

  object ModuleProject {
    def apply(name: String, settings: Seq[Setting[_]] = Seq.empty[Setting[_]]): Project = {      
      Project(
        id=name,
        base=file("modules") / name,
        settings=defaultModuleSettings ++ settings
      )
    }
  }
}