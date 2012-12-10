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

  //
  // Saves methods
  //
  def table = schema.videoAssetsCelebrity

  override def defineUpdate(theOld: VideoAssetCelebrity, theNew: VideoAssetCelebrity) = {
    updateIs(
      theOld.celebrityId := theNew.celebrityId,
      theOld.videoId := theNew.videoId)
  }
}