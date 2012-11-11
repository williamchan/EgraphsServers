import sbt._
import sbt.Keys._

/** Provides plugin source dependencies. In this case, our fork of the cloudbees deployment plugin. */
object Build extends Build {
  override lazy val projects = Seq(root)
  lazy val root = Project("plugins", file(".")) dependsOn( cloudbeesPlugin )
  lazy val cloudbeesPlugin = uri("git://github.com/Egraphs/sbt-cloudbees-play-plugin.git#config-tab-complete")
}