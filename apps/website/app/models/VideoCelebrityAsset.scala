package models

import services._
import db._
import com.google.inject.Inject
import java.sql.Timestamp
import services.{ AppConfig, Time }
import services.db.{ FilterOneTable, KeyedCaseClass, Schema, SavesWithLongKey }
import org.squeryl.Query
import com.google.inject.Inject

case class VideoCelebrityAssetServices @Inject() (store: VideoCelebrityAssetStore)

case class VideoCelebrityAsset(id: Long = 0,
  celebrityId: Long = 0,
  videoId: Long = 0,
  services: VideoCelebrityAssetServices = AppConfig.instance[VideoCelebrityAssetServices])
  extends KeyedCaseClass[Long] {

  //
  // Public members
  //
  def save(): VideoCelebrityAsset = {
    services.store.save(this)
  }

  //
  // KeyedCaseClass methods
  //
  override def unapplied = VideoCelebrityAsset.unapply(this)

}

class VideoCelebrityAssetStore @Inject() (schema: Schema) extends SavesWithLongKey[VideoCelebrityAsset] {
  import org.squeryl.PrimitiveTypeMode._

  //
  // Saves methods
  //
  def table = schema.videoCelebrityAssets

  override def defineUpdate(theOld: VideoCelebrityAsset, theNew: VideoCelebrityAsset) = {
    updateIs(
      theOld.celebrityId := theNew.celebrityId,
      theOld.videoId := theNew.videoId)
  }
}
