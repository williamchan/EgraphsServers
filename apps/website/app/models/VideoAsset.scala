package models

import java.sql.Timestamp
import services.{ AppConfig, Time }
import services.db.{ FilterOneTable, KeyedCaseClass, Schema, SavesWithLongKey }
import org.squeryl.Query
import com.google.inject.Inject

case class VideoAssetServices @Inject() (store: VideoAssetStore)

case class VideoAsset(id: Long = 0,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  url: String,
  services: VideoAssetServices = AppConfig.instance[VideoAssetServices])
  extends KeyedCaseClass[Long]
  with HasCreatedUpdated {

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
}


class VideoAssetStore @Inject() (schema: Schema)
  extends SavesWithLongKey[VideoAsset] with SavesCreatedUpdated[Long, VideoAsset] {
  
  import org.squeryl.PrimitiveTypeMode._

  //
  // Public methods
  //

  /** I think I'd use this in the CelebVideoAsset to do the join? */
  def findByVideoAsset(id: Long): Query[VideoAsset] = {
    from(schema.videoAssets)((videoAsset) => where(videoAsset.id === id) select (videoAsset))
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
      theOld.url := theNew.url)
  }

  //
  // SavesCreatedUpdated[Long,Address] methods
  //
  override def withCreatedUpdated(toUpdate: VideoAsset, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}
