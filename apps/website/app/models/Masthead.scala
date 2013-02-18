package models

import categories.{MastheadCategoryValue, CelebrityCategoryValue, CategoryValue}
import enums.{CallToActionType, HasCallToActionType, HasPublishedStatus, PublishedStatus}
import frontend.landing.LandingMasthead
import java.sql.Timestamp
import services.{AppConfig, Time}
import services.db.{Schema, SavesWithLongKey, DBSession, KeyedCaseClass}
import services.blobs.AccessPolicy
import com.google.inject.{Provider, Inject}
import org.apache.commons.io.IOUtils
import play.api.Play._
import controllers.WebsiteControllers
import org.squeryl.Query
import org.squeryl.dsl.ManyToMany
import services.mvc.celebrity.CatalogStarsQuery
import services.mvc.landing.LandingMastheadsQuery

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
  _callToActionType: String = CallToActionType.SimpleLink.name,
  callToActionTarget: String = "#",
  callToActionText: String = "",
  _publishedStatus: String = PublishedStatus.Unpublished.name,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: MastheadServices = AppConfig.instance[MastheadServices]
) extends KeyedCaseClass[Long]
  with HasCreatedUpdated
  with HasPublishedStatus[Masthead]
  with LandingPageImage[Masthead]
  with HasCallToActionType[Masthead]
{

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  override def save(): Masthead = {
    require(!headline.isEmpty, "Mastheads need headlines to be considered valid")
    services.store.save(this)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = Masthead.unapply(this)

  //
  // PublishedStatus[Masthead] methods
  //
  override def withPublishedStatus(status: PublishedStatus.EnumVal) :  Masthead = {
    this.copy(_publishedStatus = status.name)
  }

  //
  //
  // CallToAction[Masthead] methods

  override def withCallToActionType(action: CallToActionType.EnumVal) : Masthead = {
    this.copy(_callToActionType = action.name)
  }

  // LandingPageImage

  override val keyBase = "masthead/" + id

  override def withLandingPageImageKey(key: Option[String]) : Masthead = {
    this.copy(_landingPageImageKey = key)
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
  landingMastheadsQuery: LandingMastheadsQuery,
  dbSession: DBSession
) extends SavesWithLongKey[Masthead] with SavesCreatedUpdated[Masthead] {
  import org.squeryl.PrimitiveTypeMode._

  /**
   * Returns all celebrities associated with the provided CategoryValue.
   */
  def mastheads(categoryValue: CategoryValue) : Query[Masthead] with ManyToMany[Masthead, MastheadCategoryValue] = {
    schema.mastheadCategoryValues.right(categoryValue)
  }

  def getLandingMastheads : Iterable[LandingMasthead] = {
    for(masthead <- getAll) yield {
      LandingMasthead(
        id = masthead.id,
        name=masthead.name,
        headline = masthead.headline,
        subtitle = masthead.subtitle,
        landingPageImageUrl = masthead.landingPageImage.resizedWidth(1550).getSaved(services.blobs.AccessPolicy.Public).url,
        callToActionViewModel = CallToActionType.toViewModel(
          masthead.callToActionType,
          masthead.callToActionText,
          masthead.callToActionTarget
        )
      )
    }
  }

  def getAll: Iterable[Masthead] = {
    for(masthead <- schema.mastheads) yield masthead
  }
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

