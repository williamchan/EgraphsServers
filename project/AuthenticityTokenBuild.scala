import sbt._
import Keys._
import play.core.PlayVersion

import BuildHelpers.ModuleProject

object AuthenticityTokenBuild extends Build {
  val main = ModuleProject(name="authenticity-token").dependsOn(PlayUtilsBuild.main)
}