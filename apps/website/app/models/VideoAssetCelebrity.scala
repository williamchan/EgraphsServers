package models

import services._
import db._
import com.google.inject.Inject
import java.sql.Timestamp
import services.{ AppConfig, Time }
import services.db.{ FilterOneTable, KeyedCaseClass, Schema, SavesWithLongKey }
import org.squeryl.Query
import com.google.inject.Inject

case class VideoAssetCelebrityServices @Inject() (store: VideoAssetCelebrityStore)

case class VideoAssetCelebrity(id: Long = 0,
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
