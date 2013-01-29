package models

import org.squeryl.PrimitiveTypeMode.from
import org.squeryl.PrimitiveTypeMode.long2ScalarLong
import org.squeryl.PrimitiveTypeMode.optionLong2ScalarLong
import org.squeryl.PrimitiveTypeMode.where

import com.google.inject.Inject

import services.{ AppConfig, Time }
import services.db.KeyedCaseClass
import services.db.SavesWithLongKey
import services.db.Schema

case class VideoAssetCelebrityServices @Inject() (store: VideoAssetCelebrityStore)

case class VideoAssetCelebrity(
  id: Long = 0,
  celebrityId: Long = 0,
  videoId: Long = 0,
  services: VideoAssetCelebrityServices = AppConfig.instance[VideoAssetCelebrityServices])
  extends KeyedCaseClass[Long] {
  
  //
  // Public members
  //
  def save(): VideoAssetCelebrity = {
    services.store.save(this)
  }

  //
  // KeyedCaseClass methods
  //
  override def unapplied = VideoAssetCelebrity.unapply(this)

}

class VideoAssetCelebrityStore @Inject() (schema: Schema) extends SavesWithLongKey[VideoAssetCelebrity] {
  import org.squeryl.PrimitiveTypeMode._

  def getCelebrityByVideoId(videoId: Long): Option[Celebrity] = {
    val celebrityId = from(schema.videoAssetsCelebrity)(videoAssetCelebrity =>
      where(videoAssetCelebrity.videoId === videoId)
        select (videoAssetCelebrity.celebrityId)).headOption

    from(schema.celebrities)(celebrity =>
      where(celebrity.id === celebrityId)
        select (celebrity)).headOption
  }
  
  // assumes there is only 1 video asset per celebrity
  // (if that is ever not the case, this should be modified)
  def getVideoAssetByCelebrityId(celebrityId: Long): Option[VideoAsset] = {
    val videoAssetId = from(schema.videoAssetsCelebrity)(videoAssetCelebrity =>
      where(videoAssetCelebrity.celebrityId === celebrityId)
        select (videoAssetCelebrity.videoId)).headOption
        
    from(schema.videoAssets)(videoAsset =>
      where(videoAsset.id === videoAssetId)
        select (videoAsset)).headOption
  }

  //
  // Saves methods
  //
  def table = schema.videoAssetsCelebrity


}