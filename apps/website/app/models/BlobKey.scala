package models

import com.google.inject.Inject
import java.sql.Timestamp
import services._
import db.{Schema, Saves, KeyedCaseClass}

case class BlobKeyServices @Inject()(store: BlobKeyStore)

// We have stopped using this table.
@Deprecated
case class BlobKey(id: Long = 0L,
                   key: String = "",
                   url: String = "",
                   created: Timestamp = Time.defaultTimestamp,
                   updated: Timestamp = Time.defaultTimestamp,
                   services: BlobKeyServices = AppConfig.instance[BlobKeyServices]
                    ) extends KeyedCaseClass[Long] with HasCreatedUpdated {

  //
  // Public members
  //
  def save(): BlobKey = {
    services.store.save(this)
  }

  //
  // KeyedCaseClass methods
  //
  override def unapplied = {
    BlobKey.unapply(this)
  }

}

class BlobKeyStore @Inject()(schema: Schema) extends Saves[BlobKey] with SavesCreatedUpdated[BlobKey] {
  import org.squeryl.PrimitiveTypeMode._

  def findByKey(key: String): Option[BlobKey] = {
    from(table)(blobKey => where(blobKey.key === key) select (blobKey)).headOption
  }

  //
  // Saves methods
  //
  def table = schema.blobKeys

  override def defineUpdate(theOld: BlobKey, theNew: BlobKey) = {
    updateIs(
      theOld.key := theNew.key,
      theOld.url := theNew.url,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated methods
  //
  override def withCreatedUpdated(toUpdate: BlobKey, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}
