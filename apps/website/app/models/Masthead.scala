package models

import enums.{HasPublishedStatus, PublishedStatus}
import java.sql.Timestamp
import services.{AppConfig, Time}
import services.db.{Schema, SavesWithLongKey, DBSession, KeyedCaseClass}
import services.blobs.AccessPolicy
import com.google.inject.{Provider, Inject}
import org.apache.commons.io.IOUtils
import play.api.Play._
import models.MastheadServices
import models.ImageAssetServices
import models.Masthead

case class MastheadServices @Inject() (
  store: MastheadStore,
  imageAssetServices: Provider[ImageAssetServices]
)

/**
* Entity representing mastheads on the homepage
*/
case class Masthead (
  id: Long = 0,
  name: String = "",
  headline: String = "",
  subtitle: Option[String] = None,
  _landingPageImageKey: Option[String] = None,
  _publishedStatus: String = PublishedStatus.Unpublished.name,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: MastheadServices = AppConfig.instance[MastheadServices]
) extends KeyedCaseClass[Long]
  with HasCreatedUpdated
  with HasPublishedStatus[Masthead]
  with LandingPageImage[Masthead]
{

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): Masthead = {
    require(!headline.isEmpty, "You need a headline for a masthead")
    services.store.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = Masthead.unapply(this)

  // Published status

  override def withPublishedStatus(status: PublishedStatus.EnumVal) = {
    this.copy(_publishedStatus = status.name)
  }

  // LandingPageImage

  override val keyBase = "masthead/" + id

  override def withLandingPageImageKey(key: Option[String]) : Masthead = {
    this
  }

  override def imageAssetServices = services.imageAssetServices.get

  override lazy val defaultLandingPageImage = ImageAsset(
    IOUtils.toByteArray(current.resourceAsStream("images/1550x556.jpg").get),
    keyBase="defaults/masthead",
    name="landingPageImage",
    imageType=ImageAsset.Png,
    services=services.imageAssetServices.get
  )

}

class MastheadStore @Inject() (
  schema: Schema,
  dbSession: DBSession
) extends SavesWithLongKey[Masthead] with SavesCreatedUpdated[Masthead] {
  import org.squeryl.PrimitiveTypeMode._

  //
  // SavesWithLongKey[Celebrity] methods
  //
  override val table = schema.mastheads

  //
  // SavesCreatedUpdated[Celebrity] methods
  //
  override def withCreatedUpdated(toUpdate: Masthead, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}

