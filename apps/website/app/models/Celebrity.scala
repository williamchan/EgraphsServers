package models

import enums.{HasEnrollmentStatus, EnrollmentStatus, PublishedStatus, HasPublishedStatus}
import categories._
import java.sql.Timestamp
import services.blobs.AccessPolicy
import services.db.{FilterOneTable, KeyedCaseClass, Schema, SavesWithLongKey}
import services.blobs.Blobs.Conversions._
import com.google.inject.{Provider, Inject}
import org.squeryl.Query
import services._
import java.awt.image.BufferedImage
import services.mail.TransactionalMail
import org.apache.commons.io.IOUtils
import org.apache.commons.mail.HtmlEmail
import models.Celebrity.CelebrityWithImage
import play.api.Play.current
import services.Dimensions
import org.squeryl.dsl.ManyToMany
import views.html.frontend.{celebrity_welcome_email, celebrity_welcome_email_text}
import anorm._
import anorm.SqlParser._
import services.mvc.celebrity.CelebrityViewConversions
import models.frontend.marketplace.MarketplaceCelebrity

/**
 * Services used by each celebrity instance
 */
case class CelebrityServices @Inject() (
  store: CelebrityStore,
  accountStore: AccountStore,
  categoryServices: CategoryServices,
  productStore: ProductStore,
  orderStore: OrderStore,
  inventoryBatchStore: InventoryBatchStore,
  inventoryBatchQueryFilters: InventoryBatchQueryFilters,
  schema: Schema,
  productServices: Provider[ProductServices],
  imageAssetServices: Provider[ImageAssetServices],
  transactionalMail: TransactionalMail
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

  /**
   * FilterValues celebrity is tagged withs
   */

  lazy val categoryValues = services.categoryServices.categoryValueStore.categoryValues(this)
  
  lazy val categoryValueAndCategoryPairs : Query[(CategoryValue, Category)] = services.categoryServices.categoryValueStore.categoryValueCategoryPairs(this)

  //
  // Additional DB columns
  //
  /**The slug used to access this Celebrity's page on the main site. */
  val urlSlug: String = Utils.slugify(publicName, false) // Slugify without lower-casing

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

  def productsInActiveInventoryBatches(): Seq[Product] = {
    services.productStore.findActiveProductsByCelebrity(id).toSeq
  }

  @deprecated("This is still here because SER-86 does not work yet.", "")
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

  /**
  * Sends a welcome email to the celebrities email address with their Egraphs username and a blanked
  * out password field.  We aren't sending the password, it is just a bunch of *****.  The email
  * includes a link to download the latest iPad app.
  */
  def sendWelcomeEmail(toAddress: String, bccEmail: Option[String] = None) {
    val email = new HtmlEmail()

    email.setFrom("noreply@egraphs.com", "Egraphs")
    email.addTo(toAddress, publicName)
    bccEmail.map(bcc => email.addBcc(bcc))
    email.setSubject("Welcome to Egraphs")

    services.transactionalMail.send(
      email, 
      text=Some(celebrity_welcome_email_text(publicName, account.email).toString), 
      html=Some(celebrity_welcome_email(publicName, account.email))
    )
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
    require(id > 0, "Cannot determine blobstore key when no id exists yet for this entity in the relational database")
    "celebrity/" + id
  }

  lazy val defaultProfile = ImageAsset(
    IOUtils.toByteArray(current.resourceAsStream("images/default_profile.jpg").get),
    keyBase="defaults/celebrity",
    name="profilePhoto",
    imageType=ImageAsset.Png,
    services=services.imageAssetServices.get
  )
  lazy val defaultLandingPageImage = ImageAsset(
    IOUtils.toByteArray(current.resourceAsStream("images/1500x556_blank.jpg").get),
    keyBase="defaults/celebrity",
    name="landingPageImage",
    imageType=ImageAsset.Png,
    services=services.imageAssetServices.get
  )
  lazy val defaultLogoImage = ImageAsset(
    IOUtils.toByteArray(current.resourceAsStream("images/40x40_blank.jpg").get),
    keyBase="defaults/celebrity",
    name="logoImage",
    imageType=ImageAsset.Png,
    services=services.imageAssetServices.get
  )
}

object Celebrity {
  val minLandingPageImageWidth = 1500
  val minLandingPageImageHeight = 556
  val minLogoWidth = 40

  val defaultLandingPageImageDimensions = Dimensions(width = minLandingPageImageWidth, height = minLandingPageImageHeight)
  val landingPageImageAspectRatio = minLandingPageImageWidth.toDouble / minLandingPageImageHeight
  val defaultLogoDimensions = Dimensions(width = minLogoWidth, height = minLogoWidth)

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
  import CelebrityViewConversions._
  //
  // Public Methods
  //
  /**
   * Returns all celebrities associated with the provided CategoryValue.
   */
  def celebrities(categoryValue: CategoryValue) : Query[Celebrity] with ManyToMany[Celebrity, CelebrityCategoryValue] = {
    schema.celebrityCategoryValues.right(categoryValue)
  }

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
 
  /**
   * Find celebrities tagged with a particular filterValue by id.
   */
  def findByCategoryValueId(categoryValueId : Long) : Query[Celebrity] = {
   from(schema.celebrityCategoryValues, schema.celebrities)(
     (cfv, c) =>
       where(
         cfv.categoryValueId === categoryValueId and  
         c.id === cfv.celebrityId
       ) select(c)
   ) 
  }
   
  
  def rebuildSearchIndex {
    SQL(
    """
        DROP INDEX celebrity_category_search_idx;
    """
        ).execute()(connection=schema.getTxnConnectionFactory)     
    SQL(
    """    
        SELECT refresh_matview('celebrity_categories_mv');
    """
        ).execute()(connection=schema.getTxnConnectionFactory)
    SQL(
    """    
        CREATE INDEX celebrity_category_search_idx ON celebrity_categories_mv USING gin(to_tsvector);
    """
    ).execute()(connection=schema.getTxnConnectionFactory)
  }
  /**
   * Full text search on tags, this version is called by the admin controller. 
   * TODO(sbilstein) Implement refinements 
   */
  
  def search(query: String, refinements: Map[Long, Iterable[Long]] = Map[Long, Iterable[Long]]()): Iterable[Celebrity] = {
    val rowStream = SQL(
      """
      SELECT c.publicname publicname, c._landingpageImageKey _landingpageImageKey, c.id celebrityid,
        c.roledescription roledescription, 
        c._enrollmentStatus _enrollmentStatus, c._publishedStatus _publishedStatus
      FROM celebrity c, celebrity_categories_mv mv WHERE
        (
          mv.to_tsvector
          @@
          plainto_tsquery('english', {textQuery})
        ) and mv.id = c.id
      GROUP BY c.id;

      """
    ).on("textQuery" -> query).apply()(connection = schema.getTxnConnectionFactory)
    
    for( row <- rowStream) yield {
      Celebrity(
        id = row[Long]("celebrityid"),
         publicName = row[String]("publicname"),
         roleDescription = row[String]("roledescription"),                                 
         _enrollmentStatus = row[String]("_enrollmentStatus"),
         _publishedStatus = row[String]("_publishedStatus"),
         _landingPageImageKey = row[Option[String]]("_landingpageImageKey")
      )
    }
  }
  /**
   * This function is a dupe of the above function. We will probably want to think of a good way to abstract the search if we want to retain a
   * useful admin text search. 
   * TODO: implement refinements, pass in real data for sold out, minPrice, and maxPrice.
   * Currently the view conversion is doing another DB query to fill in that data for each celebrity, would be great if we could do it all in one trip. 
   * Set[Categoryid, Iterable[CategoryValueId]]
   * in each set (OR CategoryValueIds) And (OR CategoryValueIds)
   * 
   *  (pitcher or 2nd baseman) and (red sox or yankees)
   */
  def marketplaceSearch(query: String, refinements: Map[Long, Iterable[Long]] = Map[Long, Iterable[Long]]()): Iterable[MarketplaceCelebrity] = {

    // Note we could make this fast probably.  We should see how performance is affected if we don't have to
    // join across all celebrities since we can filter some with the text search first.
    val queryString = """
    SELECT
     stuff.celeb_id AS celebrityid,
     stuff.celeb_publicname AS publicname,
     stuff.celeb_roledescription AS roledescription,
     stuff.celeb_landingpageImageKey AS _landingpageImageKey,
     min(stuff.product_priceincurrency) AS minProductPrice,
     max(stuff.product_priceincurrency) AS maxProductPrice,
     stuff.inventorybatch_id,
     sum(stuff.is_order) AS inventory_sold,
     stuff.inventory_total,
     stuff.inventory_total - sum(stuff.is_order) AS inventoryAvailable
    FROM (
    
    SELECT
     c.id AS celeb_id,
     c.publicname AS celeb_publicname,
     c.roledescription AS celeb_roledescription,
     c._landingpageImageKey AS celeb_landingpageImageKey,
     p.id AS product_id,
     p.priceincurrency AS product_priceincurrency,
     ib.id AS inventorybatch_id,
     coalesce(o.id - o.id + 1, 0) AS is_order,
     ib.numInventory AS inventory_total
    FROM
     celebrity c INNER JOIN product p ON (p.celebrityid = c.id)
                 INNER JOIN inventorybatch ib ON (ib.celebrityid = c.id)
                 INNER JOIN celebrity_categories_mv mv ON (mv.id = c.id)
                 LEFT OUTER JOIN orders o ON (o.inventorybatchid = ib.id AND
                                              o.productid = p.id)
    WHERE
     c._enrollmentStatus = 'Enrolled' AND
     c._publishedStatus = 'Published' AND
     p._publishedStatus = 'Published' AND
     ib.startdate < now() AND
     ib.enddate > now() AND
     mv.to_tsvector @@ plainto_tsquery('english', {textQuery})
    ORDER BY is_order ASC
    
    ) AS stuff
    GROUP BY celeb_id, celeb_publicname, celeb_roledescription, celeb_landingpageImageKey, inventorybatch_id, inventory_total
    ORDER BY celeb_id;
    """

    val rowStream = SQL(
      queryString
    ).on("textQuery" -> query).apply()(connection = schema.getTxnConnectionFactory)

    for (row <- rowStream) yield {
      import java.math.BigDecimal
      Celebrity(
        id = row[Long]("celebrityid"),
        publicName = row[String]("publicname"),
        roleDescription = row[String]("roledescription"),
        _enrollmentStatus = EnrollmentStatus.Enrolled.name,
        _publishedStatus = PublishedStatus.Published.name,
        _landingPageImageKey = row[Option[String]]("_landingpageImageKey")
      ).asMarketplaceCelebrity(
        soldout = row[BigDecimal]("inventoryAvailable").intValue() > 0,
        minPrice = row[BigDecimal]("minProductPrice").intValue(),
        maxPrice = row[BigDecimal]("maxProductPrice").intValue()
      )
    }
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
    // First update those gentlemen that are no longer featured
    update(schema.celebrities)(c =>
      where(c.isFeatured === true and (c.id notIn newFeaturedCelebIds))
        set (c.isFeatured := false)
    )

    // Now lets feature the real stars here!
    update(schema.celebrities)(c =>
      where(c.id in newFeaturedCelebIds)
        set (c.isFeatured := true)
    )
  }

  /**
   * Update a celebrity's associated filter values
   **/

  def updateCategoryValues(celebrity: Celebrity, categoryValueIds: Iterable[Long]) {
    //remove old records
    celebrity.categoryValues.dissociateAll

    // Add records for the new values
    val newCelebrityCategoryValues  = for (categoryValueId <- categoryValueIds) yield 
    { 
      CelebrityCategoryValue(celebrityId = celebrity.id, categoryValueId = categoryValueId)
    }

    schema.celebrityCategoryValues.insert(
       newCelebrityCategoryValues
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

/**
 * Simple class for representing celebrities in lists.
 * TODO: Move into viewmodels when we refactor the admin panel.
 **/

case class CelebrityListing(
    minPrice: Int,
    maxPrice: Int,
    soldout: Boolean
)
