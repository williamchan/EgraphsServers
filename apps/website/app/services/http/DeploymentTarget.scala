package services.http

import services.Utils

/**
 * Enumerates the different deployment targets we have. Prefer using these
 * enums over hard-coding the strings.
 */
object DeploymentTarget {
  val Test = "test"

  val Staging = "staging"

  val Demo = "demo"

  val Live = "live"
}
