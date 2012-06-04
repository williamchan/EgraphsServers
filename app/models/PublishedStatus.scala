package models

import services.Utils



object PublishedStatus extends Utils.Enum {
  sealed trait EnumVal extends Value

  val Published = new EnumVal{val name = "Published"}
  val Unpublished = new EnumVal {val name = "Unpublished"}
//  PublishedStatus.
}
