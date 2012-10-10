import sbt._
import Keys._
import play.core.PlayVersion
import BuildHelpers.ModuleProject

object PlayUtilsBuild extends Build {
  val main = ModuleProject(name="play-utils", settings=Seq(version := "1.0"))
}