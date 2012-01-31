package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import services.Blobs.AccessPolicy
import org.squeryl.Query
import play.templates.JavaExtensions
import db.{FilterOneTable, KeyedCaseClass, Schema, Saves}
import services.Blobs.Conversions._
import services.{Utils, Serialization, Time}
import services.AppConfig
import com.google.inject.{Provider, Inject}

/**
 * Services used by each celebrity instance
 */
case class CelebrityServices @Inject() (
  store: CelebrityStore,
  productStore: ProductStore,
  productServices: Provider[ProductServices],
  imageAssetServices: Provider[ImageAssetServices]
)


/**
 * Persistent entity representing the Celebrities who provide products on
 * our service.
 */
case class Celebrity(id: Long = 0,
                     apiKey: Option[String] = None,
                     description: Option[String] = None,
                     firstName: Option[String] = None,
                     lastName: Option[String] = None,
                     publicName: Option[String] = None,
                     profilePhotoUpdated: Option[String] = None,
                     enrollmentStatusValue: String = NotEnrolled.value,
                     created: Timestamp = Time.defaultTimestamp,
                     updated: Timestamp = Time.defaultTimestamp,
                     services: CelebrityServices = AppConfig.instance[CelebrityServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  //
  // Additional DB columns
  //
  /**The slug used to access this Celebrity's page on the main site. */
  val urlSlug = publicName.map(name => JavaExtensions.slugify(name, false)) // Slugify without lower-casing

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): Celebrity = {
    services.store.save(this)
  }

  /** Makes a copy of the object with the new enrollment status applied. */
  def withEnrollmentStatus(newStatus: EnrollmentStatus): Celebrity = {
    copy(enrollmentStatusValue=newStatus.value)
  }

  /** The current Biometric services enrollment status. */
  def enrollmentStatus: EnrollmentStatus = {
    EnrollmentStatus(enrollmentStatusValue)
  }

  /**Returns all of the celebrity's Products */
  def products(filters: FilterOneTable[Product]*): Query[Product] = {
    services.productStore.findByCelebrity(id, filters: _*)
  }


  /**
   * Renders the Celebrity as a Map, which will itself be rendered into whichever data format
   * by the API (e.g. JSON)
   */
  def renderedForApi: Map[String, Any] = {
    val optionalFields = List(
      ("firstName" -> firstName),
      ("lastName" -> lastName),
      ("publicName" -> publicName)
    )

    Map("id" -> id,
      "enrollmentStatus" -> enrollmentStatusValue) ++
      renderCreatedUpdatedForApi ++
      Serialization.makeOptionalFieldMap(optionalFields)
  }

  /**
   * Saves the celebrity entity after first uploading the provided image
   * data as the master copy for the Celebrity's profile. The data should be in
   * `JPEG` or `PNG` format.
   *
   * @return the newly persisted celebrity with a valid profile photo.
   */
  def saveWithProfilePhoto(imageData: Array[Byte]): (Celebrity, ImageAsset) = {
    val celebrityToSave = this.copy(profilePhotoUpdated = Some(Time.toBlobstoreFormat(Time.now)))
    val assetName = celebrityToSave.profilePhotoAssetNameOption.get
    val image = ImageAsset(
      imageData, keyBase, assetName, ImageAsset.Png, services=services.imageAssetServices.get
    )

    // Upload the image then save the entity, confident that the resulting entity
    // will have a valid master image.
    image.save(AccessPolicy.Public)

    (celebrityToSave.save(), image)
  }

  /**
   * Returns the profile photo image asset for this celebrity if one was ever stored using
   * `saveWithProfilePhoto`.
   */
  def profilePhoto: ImageAsset = {
    profilePhotoAssetNameOption
      .flatMap( assetName => Some(ImageAsset(keyBase, assetName, ImageAsset.Png, services=services.imageAssetServices.get)) )
      .getOrElse(defaultProfile)
  }

  /**Creates a new Product associated with the celebrity. The product is not yet persisted. */
  def newProduct: Product = {
    Product(celebrityId=id, services=services.productServices.get)
  }

  private def getMostRecentEnrollmentBatch(): Option[EnrollmentBatch] = {
    from(Schema.enrollmentBatches)(enrollmentBatch =>
      where(enrollmentBatch.celebrityId === this.id)
        select (enrollmentBatch)
        orderBy(enrollmentBatch.created desc)
    ).headOption
  }

  def getOpenEnrollmentBatch(): Option[EnrollmentBatch] = {
    from(Schema.enrollmentBatches)(enrollmentBatch =>
      where(enrollmentBatch.celebrityId === this.id and enrollmentBatch.isSuccessfulEnrollment.isNull)
        select (enrollmentBatch)
    ).headOption
  }

  def getXyzmoUID(): String = {
    id.toString + "." + created.getTime.toString +"." + getMostRecentEnrollmentBatch().get.id
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = Celebrity.unapply(this)

  //
  // Private members
  //
  /** Blobstore folder name for stored profile photo data. */
  private def profilePhotoAssetNameOption: Option[String] = {
    for (photoUpdatedTimestamp <- profilePhotoUpdated) yield {
      "profile_" + photoUpdatedTimestamp
    }
  }

  /**
   * The blobstore folder name upon which all resources relating to this celebrity should base
   * their keys. This value can not be determined if the entity has not yet been saved.
   */
  private def keyBase = {
    require(id > 0, "Can not determine blobstore key when no id exists yet for this entity in the relational database")

    "celebrity/" + id
  }

  lazy val defaultProfile = ImageAsset(
    play.Play.getFile("test/files/longoria/profile.jpg"),
    keyBase="defaults/celebrity",
    name="profilePhoto",
    imageType=ImageAsset.Png,
    services=services.imageAssetServices.get
  )
}

class CelebrityStore @Inject() (schema: Schema) extends Saves[Celebrity] with SavesCreatedUpdated[Celebrity] {
  //
  // Public Methods
  //
  def findByUrlSlug(slug: String): Option[Celebrity] = {
    from(schema.celebrities)(celebrity =>
      where(celebrity.urlSlug === Some(slug))
        select (celebrity)
    ).headOption
  }

  //
  // Saves[Celebrity] methods
  //
  override val table = schema.celebrities

  override def defineUpdate(theOld: Celebrity, theNew: Celebrity) = {
    updateIs(
      theOld.apiKey := theNew.apiKey,
      theOld.description := theNew.description,
      theOld.firstName := theNew.firstName,
      theOld.lastName := theNew.lastName,
      theOld.publicName := theNew.publicName,
      theOld.urlSlug := theNew.urlSlug,
      theOld.profilePhotoUpdated := theNew.profilePhotoUpdated,
      theOld.enrollmentStatusValue := theNew.enrollmentStatusValue,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Celebrity] methods
  //
  override def withCreatedUpdated(toUpdate: Celebrity, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}

object Celebrity {
}

abstract sealed class EnrollmentStatus(val value: String)

object EnrollmentStatus {
  private val states = Utils.toMap[String, EnrollmentStatus](Seq(
    NotEnrolled,
    AttemptingEnrollment,
    Enrolled,
    FailedEnrollment
  ), key=(theState) => theState.value)

  /** Provides the EnrollmentStatus object that maps to the provided string. */
  def apply(value: String) = {
     states(value)
  }
}

case object NotEnrolled extends EnrollmentStatus("NotEnrolled")

case object AttemptingEnrollment extends EnrollmentStatus("AttemptingEnrollment")

case object Enrolled extends EnrollmentStatus("Enrolled")

case object FailedEnrollment extends EnrollmentStatus("FailedEnrollment")
