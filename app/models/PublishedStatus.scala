package models

import services.Utils



object PublishedStatus extends Utils.Enum {
  sealed trait EnumVal extends Value

  val Published = new EnumVal{val name = "Published"}
  val Unpublished = new EnumVal {val name = "Unpublished"}

}

trait HasPublishedStatus[T] {
  def _publishedStatus: String

  def publishedStatus: PublishedStatus.EnumVal = {
    PublishedStatus(_publishedStatus).getOrElse(
      throw new IllegalArgumentException(_publishedStatus)
    )
  }

  def withPublishedStatus(status: PublishedStatus.EnumVal) : T

}

