package models

import enums.{HasEnrollmentStatus, EnrollmentStatus, PublishedStatus, HasPublishedStatus}
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
                     description: Option[String] = None,
                     firstName: Option[String] = None,
                     lastName: Option[String] = None,
                     publicName: Option[String] = None,
                     profilePhotoUpdated: Option[String] = None,
                     _enrollmentStatus: String = EnrollmentStatus.NotEnrolled.name,
                     isFeatured: Boolean = false,
                     roleDescription: Option[String] = None, // e.g. "Pitcher, Red Sox"
                     _publishedStatus: String = PublishedStatus.Unpublished.name,
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
  val urlSlug: Option[String] = publicName.map(name => JavaExtensions.slugify(name, false)) // Slugify without lower-casing

  //
  // Public members
  //
  /**Persists by conveniently delegating to companion object's save method. */
  def save(): Celebrity = {
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
    val inventoryBatchIdsAndOrderCounts: Map[Long, Int] = Map(services.orderStore.countOrdersByInventoryBatch(inventoryBatchIds): _*)
    val inventoryBatchIdsAndNumRemaining: Map[Long, Int] = for ((batchId, orderCount) <- inventoryBatchIdsAndOrderCounts) yield {
      (batchId, (inventoryBatches.get(batchId).get.numInventory - orderCount))
    }

    // 3) query for products and inventoryBatchProducts on inventoryBatchIds
    val productsAndBatchIdsQuery: Query[(Product, Long)] = services.productStore.getProductAndInventoryBatchAssociations(inventoryBatchIds)
    // TODO - This implementation using a mutable map should be rewritten to use an immutable Map.
    val productsAndBatchIds = scala.collection.mutable.Map[Product, Set[Long]]()
    for ((product, batchId) <- productsAndBatchIdsQuery) {
      productsAndBatchIds.get(product) match {
        case None => productsAndBatchIds.put(product, Set(batchId))
        case Some(set) => productsAndBatchIds.put(product, set + batchId)
      }
    }

    // 4) for each product, sum quantity remaining for each associated batch
    val productsWithInventoryRemaining: scala.collection.mutable.Map[Product, Int] = productsAndBatchIds.map(b => {
      val totalNumRemainingForProduct = (for (batchId <- b._2) yield inventoryBatchIdsAndNumRemaining.get(batchId).getOrElse(0)).sum
      (b._1, totalNumRemainingForProduct)
    })
    productsWithInventoryRemaining.toSeq
  }

  def productsInActiveInventoryBatches(): Seq[Product] = {
    services.productStore.findActiveProductsByCelebrity(id).toSeq
  }

  def ordersRecentlyFulfilled: Iterable[FulfilledProductOrder] = {
    services.orderStore.findMostRecentlyFulfilledByCelebrity(this.id)
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
      "urlSlug" -> urlSlug
    )

    Map("id" -> id, "enrollmentStatus" -> enrollmentStatus.name) ++
      renderCreatedUpdatedForApi ++
      Utils.makeOptionalFieldMap(optionalFields)
  }

  def category: String = {
    "{This should be the category}"
  }

  def categoryRole: String = {
    "{This should be the category role}"
  }

  def bio: String = {
    "{This should be the bio}"
  }

  def twitterUsername: Option[String] = {
    Some("DAVIDprice14")
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
    play.Play.getFile("public/images/default_profile.jpg"),
    keyBase="defaults/celebrity",
    name="profilePhoto",
    imageType=ImageAsset.Png,
    services=services.imageAssetServices.get
  )
}

class CelebrityStore @Inject() (schema: Schema) extends Saves[Celebrity] with SavesCreatedUpdated[Celebrity] {
  import org.squeryl.PrimitiveTypeMode._

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

  def getAll: Iterable[Celebrity] = {
    for (celeb <- schema.celebrities) yield celeb
  }

  def updateFeaturedCelebrities(newFeaturedCelebIds: Iterable[Long]) {
    // First update those gentlemen that are no longer featured
    update(schema.celebrities)(c =>
      where(c.isFeatured === true and (c.id notIn newFeaturedCelebIds))
      set(c.isFeatured := false)
    )

    // Now lets feature the real stars here!
    update(schema.celebrities)(c =>
      where(c.id in newFeaturedCelebIds)
        set(c.isFeatured := true)
    )
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
      theOld._enrollmentStatus := theNew._enrollmentStatus,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated,
      theOld.isFeatured := theNew.isFeatured,
      theOld.roleDescription := theNew.roleDescription,
      theOld._publishedStatus := theNew._publishedStatus
    )
  }

  //
  // SavesCreatedUpdated[Celebrity] methods
  //
  override def withCreatedUpdated(toUpdate: Celebrity, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}
