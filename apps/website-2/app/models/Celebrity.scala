package models

import enums.{HasEnrollmentStatus, EnrollmentStatus, PublishedStatus, HasPublishedStatus}
import java.sql.Timestamp
import services.blobs.AccessPolicy
import play.templates.JavaExtensions
import services.db.{FilterOneTable, KeyedCaseClass, Schema, SavesWithLongKey}
import services.blobs.Blobs.Conversions._
import com.google.inject.{Provider, Inject}
import org.squeryl.Query
import services._
import java.awt.image.BufferedImage
import models.Celebrity.CelebrityWithImage
import play.api.Play.current

/**
 * Services used by each celebrity instance
 */
case class CelebrityServices @Inject() (
  store: CelebrityStore,
  accountStore: AccountStore,
  productStore: ProductStore,
  orderStore: OrderStore,
  inventoryBatchStore: InventoryBatchStore,
  inventoryBatchQueryFilters: InventoryBatchQueryFilters,
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
                     publicName: String = "",
                     casualName: Option[String] = None,                             // e.g. "David" instead of "David Price"
                     organization: String = "",                                     // e.g. "Major League Baseball"
                     bio: String = "",
                     roleDescription: String = "",                                  // e.g. "Pitcher, Red Sox"
                     twitterUsername: Option[String] = None,
                     profilePhotoUpdated: Option[String] = None, // todo: rename to _profilePhotoKey
                     _enrollmentStatus: String = EnrollmentStatus.NotEnrolled.name,
                     isFeatured: Boolean = false,
                     _publishedStatus: String = PublishedStatus.Unpublished.name,
                     _landingPageImageKey: Option[String] = None,
                     _logoImageKey: Option[String] = None,
                     created: Timestamp = Time.defaultTimestamp,
                     updated: Timestamp = Time.defaultTimestamp,
                     services: CelebrityServices = AppConfig.instance[CelebrityServices]
) extends KeyedCaseClass[Long]
  with HasCreatedUpdated
  with HasPublishedStatus[Celebrity]
  with HasEnrollmentStatus[Celebrity]
{
  //
  // Additional DB columns
  //
  /**The slug used to access this Celebrity's page on the main site. */
  val urlSlug: String = JavaExtensions.slugify(publicName, false) // Slugify without lower-casing

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): Celebrity = {
    require(!publicName.isEmpty, "A celebrity without a publicName is hardly a celebrity at all.")
    services.store.save(this)
  }

  def account: Account = {
    services.accountStore.findByCelebrityId(id).get
  }

  /**Returns all of the celebrity's Products */
  def products(filters: FilterOneTable[Product]*): Query[Product] = {
    services.productStore.findByCelebrity(id, filters: _*)
  }

  /**
   * Gets this Celebrity's Products that can be purchased along with the quantity available of each Product.
   * Excludes Products that are not available, such as those that are not in an active InventoryBatch based on
   * startDate and endDate, as well as those that are sold out of quantity.
   *
   * This implementation is hopefully more performant than getting all active Products and then calculating the
   * quantity available for each Product, but this assumption has yet to be tested.
   *
   * This implementation executes 3 queries, the first for the InventoryBatches, the second to aid in calculating the
   * quantity available to each InventoryBatch, and the third to get the Products and their InventoryBatch associations.
   *
   * @return a sequence of purchase-able Products along with the available quantity of each Product.
   */
  def getActiveProductsWithInventoryRemaining(): Seq[(Product, Int)] = {
    // 1) query for active InventoryBatches
    val activeIBs = services.inventoryBatchStore.findByCelebrity(id, services.inventoryBatchQueryFilters.activeOnly)
    val inventoryBatches = Utils.toMap(activeIBs, key=(theIB: InventoryBatch) => theIB.id)
    val inventoryBatchIds = inventoryBatches.keys.toList

    // 2) calculate quantity remaining for each InventoryBatch
    val inventoryBatchIdsAndOrderCounts: Map[Long, Int] =
      Map(services.orderStore.countOrdersByInventoryBatch(inventoryBatchIds): _*)

    val inventoryBatchIdsAndNumRemaining : Map[Long, Int] = inventoryBatchIds.map{ id =>
        (id, inventoryBatches.get(id).get.numInventory - inventoryBatchIdsAndOrderCounts.get(id).getOrElse(0))
      }.toMap

    // 3) query for products and inventoryBatchProducts on inventoryBatchIds
    val productsAndBatchAssociations: Query[(Product, Long)] =
      services.productStore.getProductAndInventoryBatchAssociations(inventoryBatchIds)

    val productsAndBatchIds = productsAndBatchAssociations.toMap.keySet.map(product =>
      {
       val ids =  for((p, id) <- productsAndBatchAssociations if p.id == product.id) yield Set(id)
       (product, ids.reduceLeft((s1, s2) => s1 | s2 ))
      }
    ).toMap

    // 4) for each product, sum quantity remaining for each associated batch
    val productsWithInventoryRemaining: Map[Product, Int] = productsAndBatchIds.map(b => {
      val totalNumRemainingForProduct = (for (batchId <- b._2) yield inventoryBatchIdsAndNumRemaining.get(batchId).getOrElse(0)).sum
      (b._1, totalNumRemainingForProduct)
    })
    productsWithInventoryRemaining.toSeq
  }

  def productsInActiveInventoryBatches(): Seq[Product] = {
    services.productStore.findActiveProductsByCelebrity(id).toSeq
  }

  /**
   * The orders sorted by most recently fulfilled.
   */
  def ordersRecentlyFulfilled: Iterable[FulfilledProductOrder] = {
    services.orderStore.findMostRecentlyFulfilledByCelebrity(this.id)
  }

  /**
   * Renders the Celebrity as a Map, which will itself be rendered into whichever data format
   * by the API (e.g. JSON)
   */
  def renderedForApi: Map[String, Any] = {
    Map("id" -> id, "enrollmentStatus" -> enrollmentStatus.name,  "publicName" -> publicName,
      "urlSlug" -> urlSlug) ++
      renderCreatedUpdatedForApi

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
    val image = ImageAsset(imageData, keyBase, assetName, ImageAsset.Png, services=services.imageAssetServices.get)
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

  def withLandingPageImage(imageData: Array[Byte]): CelebrityWithImage = {
    val newImageKey = "landing_" + Time.toBlobstoreFormat(Time.now)
    CelebrityWithImage(
      celebrity=this.copy(_landingPageImageKey=Some(newImageKey)),
      image=ImageAsset(imageData, keyBase, newImageKey, ImageAsset.Png, services.imageAssetServices.get)
    ).save()
  }
  def landingPageImage: ImageAsset = {
    _landingPageImageKey.flatMap(theKey => Some(ImageAsset(keyBase, theKey, ImageAsset.Png, services=services.imageAssetServices.get))) match {
      case Some(imageAsset) => imageAsset
      case None => defaultLandingPageImage
    }
  }

  def withLogoImage(imageData: Array[Byte]): CelebrityWithImage = {
    val newImageKey = "logo_" + Time.toBlobstoreFormat(Time.now)
    CelebrityWithImage(
      celebrity=this.copy(_logoImageKey=Some(newImageKey)),
      image=ImageAsset(imageData, keyBase, newImageKey, ImageAsset.Png, services.imageAssetServices.get)
    ).save()
  }
  def logoImage: ImageAsset = {
    _logoImageKey.flatMap(theKey => Some(ImageAsset(keyBase, theKey, ImageAsset.Png, services=services.imageAssetServices.get))) match {
      case Some(imageAsset) => imageAsset
      case None => defaultLogoImage
    }
  }

  def saveWithImageAssets(/*profileImage: Option[BufferedImage], */ landingPageImage: Option[BufferedImage], logoImage: Option[BufferedImage]): Celebrity = {
    import ImageUtil.Conversions._
    // todo: refactor profile image saving to here
    var celebrity = landingPageImage match {
      case None => this
      case Some(image) => {
        // todo(wchan): crop image?
        val landingPageImageBytes = image.asByteArray(ImageAsset.Jpeg)
        withLandingPageImage(landingPageImageBytes).save().celebrity
      }
    }
    celebrity = logoImage match {
      case None => this
      case Some(image) => {
        val iconImageBytes = image.asByteArray(ImageAsset.Jpeg)
        celebrity.withLogoImage(iconImageBytes).save().celebrity
      }
    }
    celebrity
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
                 image: Option[BufferedImage],
                 icon: Option[BufferedImage],
                 storyTitle: String,
                 storyText: String,
                 publishedStatus: PublishedStatus.EnumVal = PublishedStatus.Unpublished
                  ): Product =
  {
    // Create the product without blobstore images, but don't save.
    val product = Product(
      celebrityId=id,
      priceInCurrency=priceInCurrency,
      name=name,
      description=description,
      storyTitle=storyTitle,
      storyText=storyText,
      services=services.productServices.get
    ).withPublishedStatus(publishedStatus)

    product.saveWithImageAssets(image, icon)
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = Celebrity.unapply(this)

  //
  // PublishedStatus[Celebrity] methods
  //
  override def withPublishedStatus(status: PublishedStatus.EnumVal) = {
    this.copy(_publishedStatus = status.name)
  }

  override def withEnrollmentStatus(status: EnrollmentStatus.EnumVal) = {
    this.copy(_enrollmentStatus = status.name)
  }

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
    play.api.Play.getFile("public/images/default_profile.jpg"),
    keyBase="defaults/celebrity",
    name="profilePhoto",
    imageType=ImageAsset.Png,
    services=services.imageAssetServices.get
  )
  lazy val defaultLandingPageImage = ImageAsset(
    play.api.Play.getFile("public/images/1500x556_blank.jpg"),
    keyBase="defaults/celebrity",
    name="landingPageImage",
    imageType=ImageAsset.Png,
    services=services.imageAssetServices.get
  )
  lazy val defaultLogoImage = ImageAsset(
    play.api.Play.getFile("public/images/40x40_blank.jpg"),
    keyBase="defaults/celebrity",
    name="logoImage",
    imageType=ImageAsset.Png,
    services=services.imageAssetServices.get
  )
}

object Celebrity {
  val defaultLandingPageImageDimensions = Dimensions(width = minLandingPageImageWidth, height = minLandingPageImageHeight)
  val landingPageImageAspectRatio = minLandingPageImageWidth.toDouble / minLandingPageImageHeight
  val defaultLogoDimensions = Dimensions(width = minLogoWidth, height = minLogoWidth)
  val minLandingPageImageWidth = 1500
  val minLandingPageImageHeight = 556
  val minLogoWidth = 40

  // Similar to ProductWithPhoto
  case class CelebrityWithImage(celebrity: Celebrity, image: ImageAsset) {
    def save(): CelebrityWithImage = {
      val savedImage = image.save(AccessPolicy.Public)
      val saved = celebrity.save()
      CelebrityWithImage(saved, savedImage)
    }
  }
}

class CelebrityStore @Inject() (schema: Schema) extends SavesWithLongKey[Celebrity] with SavesCreatedUpdated[Long,Celebrity] {
  import org.squeryl.PrimitiveTypeMode._

  //
  // Public Methods
  //
  def findByUrlSlug(slug: String): Option[Celebrity] = {
    if (slug.isEmpty) return None

    from(schema.celebrities)(celebrity =>
      where(celebrity.urlSlug === slug)
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
        orderBy(c.id desc)
    )
    celebrityAccounts
  }

  def getFeaturedPublishedCelebrities: Iterable[Celebrity] = {
    from(schema.celebrities)( c =>
      where(c.isFeatured === true and c._publishedStatus === PublishedStatus.Published.name)
      select (c)
    )
  }

  /**
   * Returns all celebrities that should be discoverable by visitors on the website, and no
   * celebrities that should not be discoverable.
   *
   * Returns them ordered alphabetically by their public name.
   **/
  def getPublishedCelebrities: Iterable[Celebrity] = {
    from(schema.celebrities)(c =>
      where(c._publishedStatus === PublishedStatus.Published.name)
      select (c)
      orderBy (lower(c.roleDescription))
    )
  }

  def getAll: Iterable[Celebrity] = {
    for (celeb <- schema.celebrities) yield celeb
  }

  def updateFeaturedCelebrities(newFeaturedCelebIds: Iterable[Long]) {
    // newFeaturedCelebIds can apparently be null
    // TODO: find where the source of null was and remove it; we should not have null checks in this code.
    val safeNewFeaturedCelebIds = if (newFeaturedCelebIds != null) newFeaturedCelebIds else List.empty[Long]
    // First update those gentlemen that are no longer featured
    update(schema.celebrities)(c =>
      where(c.isFeatured === true and (c.id notIn safeNewFeaturedCelebIds))
        set (c.isFeatured := false)
    )

    // Now lets feature the real stars here!
    update(schema.celebrities)(c =>
      where(c.id in safeNewFeaturedCelebIds)
        set (c.isFeatured := true)
    )
  }

  //
  // SavesWithLongKey[Celebrity] methods
  //
  override val table = schema.celebrities

  override def defineUpdate(theOld: Celebrity, theNew: Celebrity) = {
    updateIs(
      theOld.apiKey := theNew.apiKey,
      theOld.bio := theNew.bio,
      theOld.casualName := theNew.casualName,
      theOld.isFeatured := theNew.isFeatured,
      theOld.organization := theNew.organization,
      theOld.profilePhotoUpdated := theNew.profilePhotoUpdated,
      theOld.publicName := theNew.publicName,
      theOld.roleDescription := theNew.roleDescription,
      theOld.twitterUsername := theNew.twitterUsername,
      theOld.urlSlug := theNew.urlSlug,
      theOld._enrollmentStatus := theNew._enrollmentStatus,
      theOld._publishedStatus := theNew._publishedStatus,
      theOld._landingPageImageKey := theNew._landingPageImageKey,
      theOld._logoImageKey := theNew._logoImageKey,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Long,Celebrity] methods
  //
  override def withCreatedUpdated(toUpdate: Celebrity, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}
