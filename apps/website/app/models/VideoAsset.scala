package models

import java.sql.Timestamp

import org.squeryl.PrimitiveTypeMode.from
import org.squeryl.PrimitiveTypeMode.long2ScalarLong
import org.squeryl.PrimitiveTypeMode.string2ScalarString
import org.squeryl.PrimitiveTypeMode.timestamp2ScalarTimestamp
import org.squeryl.PrimitiveTypeMode.where

import com.google.inject.Inject

import enums.HasVideoStatus
import models.enums.HasVideoStatus
import models.enums.VideoStatus
import services.{ AppConfig, Time }
import services.db.KeyedCaseClass
import services.db.SavesWithLongKey
import services.db.Schema
import services.blobs.Blobs
import services.Time.IntsToSeconds.intsToSecondDurations

case class VideoAssetServices @Inject() (store: VideoAssetStore, blobs: Blobs)

case class VideoAsset(
  id: Long = 0,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  url: Option[String] = None,
  urlKey: String = "",
  _videoStatus: String = VideoStatus.Unprocessed.name,
  services: VideoAssetServices = AppConfig.instance[VideoAssetServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated
  with HasVideoStatus[VideoAsset] {

  // blobkey pattern
  // (throws ERROR: null value in column "blobkeybase" violates not-null constraint
  //     if this is defined as a lazy val)
  private def blobKeyBase = "videoassets/" + id + "/"

  //
  // Public members
  //
  def save(): VideoAsset = {
    services.store.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = VideoAsset.unapply(this)

  //
  // VideoStatus[VideoAsset] methods
  //
  override def withVideoStatus(status: VideoStatus.EnumVal) = {
    this.copy(_videoStatus = status.name)
  }

  def getSecureTemporaryUrl: Option[String] = {
    val maybeSecureUrl = services.blobs.getSecureUrlOption(this.urlKey, 60 minutes)
    maybeSecureUrl match {
      case None => play.Logger.info("No video asset found with urlKey: " + this.urlKey)
      case Some(newUrl) => play.Logger.info("This video asset's URL: " + newUrl)
    }
    maybeSecureUrl
  }

  def setVideoUrlKey(filename: String): String = {
    val blobKey = blobKeyBase + filename
    this.copy(urlKey = blobKey).save()
    blobKey
  }
}

class VideoAssetStore @Inject() (schema: Schema)
  extends SavesWithLongKey[VideoAsset] with SavesCreatedUpdated[Long, VideoAsset] {

  import org.squeryl.PrimitiveTypeMode._

  def getVideosWithStatus(status: VideoStatus.EnumVal): List[VideoAsset] = {

    val queryResult = from(schema.videoAssets)(videoAsset =>
      where(videoAsset._videoStatus === status.name)
        select (videoAsset))

    queryResult.toList
  }

  //
  // SavesWithLongKey[Address] methods
  //
  override val table = schema.videoAssets

  override def defineUpdate(theOld: VideoAsset, theNew: VideoAsset) = {
    updateIs(
      theOld.id := theNew.id,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated,
      theOld.url := theNew.url,
      theOld.urlKey := theNew.urlKey,
      theOld._videoStatus := theNew._videoStatus)
  }

  //
  // SavesCreatedUpdated[Long,Address] methods
  //
  override def withCreatedUpdated(toUpdate: VideoAsset, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}
