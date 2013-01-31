package models.enums

sealed abstract class VideoType(val extension: String)

case object Mp4 extends VideoType("mp4")

case object Webm extends VideoType("webm")