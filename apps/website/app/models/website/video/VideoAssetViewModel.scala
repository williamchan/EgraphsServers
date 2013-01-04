package models.website.video

import java.util.Date

case class VideoAssetViewModel(
  videoUrl: String,
  videoId: Long,
  celebrityPublicName: String,
  created: Date
)