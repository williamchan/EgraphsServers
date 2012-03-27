package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import services.blobs.AccessPolicy
import play.templates.JavaExtensions
import services.db.{FilterOneTable, KeyedCaseClass, Schema, Saves}
import services.blobs.Blobs.Conversions._
import com.google.inject.{Provider, Inject}
import org.squeryl.Query
import services._
import java.awt.image.BufferedImage


/**
 * Services used by each celebrity instance
 */
case class CelebrityServices @Inject() (
  store: CelebrityStore,
  productStore: ProductStore,
  schema: Schema,
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
                     enrollmentStatusValue: String = EnrollmentStatus.NotEnrolled.value,
                     isLeftHanded: Boolean = false,
                     created: Timestamp = Time.defaultTimestamp,
                     updated: Timestamp = Time.defaultTimestamp,
                     services: CelebrityServices = AppConfig.instance[CelebrityServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  //
  // Additional DB columns
  //
  /**The slug used to access this Celebrity's page on the main site. */
  val urlSlug: Option[String] = publicName.map(name => JavaExtensions.slugify(name, false)) // Slugify without lower-casing

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
  def products(filters: FilterOneTable[Product]*): Iterable[Product] = {
    services.productStore.findByCelebrity(id, filters: _*)
  }


  /**
   * Renders the Celebrity as a Map, which will itself be rendered into whichever data format
   * by the API (e.g. JSON)
   */
  def renderedForApi: Map[String, Any] = {
    val optionalFields = List(
      "firstName" -> firstName,
      "lastName" -> lastName,
      "publicName" -> publicName,
      "urlSlug" -> urlSlug,
      "isLeftHanded" -> Some(isLeftHanded)
    )

    Map("id" -> id, "enrollmentStatus" -> enrollmentStatusValue) ++
      renderCreatedUpdatedForApi ++
      Utils.makeOptionalFieldMap(optionalFields)
  }

  /**
   * Saves the celebrity entity after first uploading the provided image
   * data as the master copy for the Celebrity's profile. The data should be in
   * `JPEG` or `PNG` format.
   *
   * The celebrity must have been previously persisted because an id is required.
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

  /**
   * Adds the provided product to the Celebrity's collection of products.
   * Returns the persisted product.
   **/
  def addProduct(name: String,
                 description: String,
                 priceInCurrency: BigDecimal=Product.defaultPrice,
                 image: BufferedImage,
                 icon: BufferedImage,
                 storyTitle: String,
                 storyText: String): Product =
  {
    import ImageUtil.Conversions._

    // Create the product without blobstore images, but don't save.
    val product = Product(
      celebrityId=id,
      priceInCurrency=priceInCurrency,
      name=name,
      description=description,
      storyTitle=storyTitle,
      storyText=storyText,
      services=services.productServices.get
    )

    // Prepare the product photo, cropped to the suggested frame
    val frame = EgraphFrame.suggestedFrame(Dimensions(image.getWidth, image.getHeight))
    val imageCroppedToFrame = frame.cropImageForFrame(image)
    // todo(wchan): Jpeg or PNG
    val imageByteArray = imageCroppedToFrame.asByteArray(ImageAsset.Jpeg)

    // Prepare the product plaque icon, cropped to a square
    val iconCroppedToSquare = ImageUtil.cropToSquare(icon)
    val iconBytes = iconCroppedToSquare.asByteArray(ImageAsset.Jpeg)

    // Save the product so it has an ID for blobstore to key on, then add blobstore values and save again
    val savedWithFrame = product.withFrame(frame).save()
    val savedWithPhoto = savedWithFrame.withPhoto(imageByteArray).save().product

    savedWithPhoto.withIcon(iconBytes).save().product
  }

  def getMostRecentEnrollmentBatch(): Option[EnrollmentBatch] = {
    from(services.schema.enrollmentBatches)(enrollmentBatch =>
      where(enrollmentBatch.celebrityId === this.id)
        select (enrollmentBatch)
        orderBy(enrollmentBatch.created desc)
    ).headOption
  }

  def getOpenEnrollmentBatch(): Option[EnrollmentBatch] = {
    from(services.schema.enrollmentBatches)(enrollmentBatch =>
      where(enrollmentBatch.celebrityId === this.id and enrollmentBatch.isSuccessfulEnrollment.isNull)
        select (enrollmentBatch)
    ).headOption
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
    if (slug.isEmpty) return None

    from(schema.celebrities)(celebrity =>
      where(celebrity.urlSlug === Some(slug))
        select (celebrity)
    ).headOption
  }

  // TODO(erem): Test this one
  def findByEgraphId(egraphId: Long): Option[Celebrity] = {
    from(schema.celebrities, schema.products, schema.orders, schema.egraphs)(
      (c, p, o, e) =>
        where(
          e.id === egraphId and
          e.orderId === o.id and
          o.productId === p.id and
          p.celebrityId === c.id
        )
        select(c)
    ).headOption
  }

  def findByOrderId(orderId: Long): Option[Celebrity] = {
    from(schema.celebrities, schema.products, schema.orders)(
      (c, p, o) =>
        where(
          o.id === orderId and
          o.productId === p.id and
          p.celebrityId === c.id
        )
        select(c)
    ).headOption
  }

  def getCelebrityAccounts: Query[(Celebrity, Account)] = {
    val celebrityAccounts: Query[(Celebrity, Account)] = from(schema.celebrities, schema.accounts)(
      (c, a) =>
        where (c.id === a.celebrityId)
        select(c, a)
    )
    celebrityAccounts
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
      theOld.isLeftHanded := theNew.isLeftHanded,
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

abstract sealed class EnrollmentStatus(val value: String)

object EnrollmentStatus {
  case object NotEnrolled extends EnrollmentStatus("NotEnrolled")
  case object AttemptingEnrollment extends EnrollmentStatus("AttemptingEnrollment")
  case object Enrolled extends EnrollmentStatus("Enrolled")
  case object FailedEnrollment extends EnrollmentStatus("FailedEnrollment")

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