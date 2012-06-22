package services.http

import services.Utils

/**
 * Enumerates the different deployment targets we have. Prefer using these
 * enums over hard-coding the strings.
 */
object DeploymentTarget extends Utils.Enum {
  sealed trait EnumVal extends Value

  val Test = new EnumVal {
    val name = "test"
  }

  val Staging = new EnumVal {
    val name = "staging"
  }

  val Demo = new EnumVal {
    val name = "demo"
  }

  val Live = new EnumVal {
    val name = "live"
  }
}
