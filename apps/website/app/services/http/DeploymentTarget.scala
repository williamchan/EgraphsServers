package services.http

import services.Utils


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
