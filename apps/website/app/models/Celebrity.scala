package models

import java.awt.image.BufferedImage
import java.sql.Timestamp
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import com.google.inject.{Provider, Inject}
import org.joda.time.DateTimeConstants
import org.squeryl.Query
import org.squeryl.dsl.ManyToMany
import anorm._
import play.api.Play.current
import play.api.libs.concurrent._
import play.api.libs.json._
import enums.{HasEnrollmentStatus, EnrollmentStatus, PublishedStatus, HasPublishedStatus}
import categories._
import services.blobs.AccessPolicy
import services.db.{FilterOneTable, KeyedCaseClass, Schema, SavesWithLongKey}
import services._
import services.mail.TransactionalMail
import services.Dimensions
import services.mvc.celebrity.CelebrityViewConversions
import services.db.DBSession
import models.frontend.landing.CatalogStar
import models.frontend.marketplace.MarketplaceCelebrity
import models.Celebrity.CelebrityWithImage
import services.mvc.celebrity.CatalogStarsQuery
import egraphs.playutils.{Gender, HasGender}

/**
 * Services used by each celebrity instance
 */
case class CelebrityServices @Inject() (
  store: CelebrityStore,
  accountStore: AccountStore,
  consumerApp: ConsumerApplication,
  categoryServices: CategoryServices,
  celebritySecureInfoStore: CelebritySecureInfoStore,
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
case class Celebrity(
  id: Long = 0,
  apiKey: Option[String] = None,
  publicName: String = "",
  casualName: Option[String] = None,                             // e.g. "David" instead of "David Price"
  organization: String = "",                                     // e.g. "Major League Baseball"
  bio: String = "",
  roleDescription: String = "",                                  // e.g. "Pitcher, Red Sox"
  twitterUsername: Option[String] = None,
  facebookUrl: Option[String] = None,
  websiteUrl: Option[String] = None,
  profilePhotoUpdated: Option[String] = None, //TODO: rename to _profilePhotoKey
  expectedOrderDelayInMinutes: Int = 30 * DateTimeConstants.MINUTES_PER_DAY,
  _gender: String = Gender.Male.name,
  _enrollmentStatus: String = EnrollmentStatus.NotEnrolled.name,
  _publishedStatus: String = PublishedStatus.Unpublished.name,
  _landingPageImageKey: Option[String] = None,
  _logoImageKey: Option[String] = None,
  secureInfoId: Option[Long] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: CelebrityServices = AppConfig.instance[CelebrityServices]
) extends KeyedCaseClass[Long]
  with HasCreatedUpdated
  with HasPublishedStatus[Celebrity]
  with HasEnrollmentStatus[Celebrity]
  with HasGender[Celebrity]
  with LandingPageImage[Celebrity]
{

  lazy val categoryValues = services.categoryServices.categoryValueStore.categoryValues(this)
  
  lazy val categoryValueAndCategoryPairs : Query[(CategoryValue, Category)] = services.categoryServices.categoryValueStore.categoryValueCategoryPairs(this)

  //
  // Additional DB columns
  //
  /** The slug used to access this Celebrity's page on the main site. */
  val urlSlug: String = Utils.slugify(publicName, false) // Slugify without lower-casing

  //
  // Public members
  //
  /** Persists by conveniently delegating to companion object's save method. */
  override def save(): Celebrity = {
    require(!publicName.isEmpty, "A celebrity without a publicName is hardly a celebrity at all.")
    services.store.save(this)
  }

  def account: Account = {
    services.accountStore.findByCelebrityId(id).get
  }

  /** Returns all of the celebrity's Products */
  def products(filters: FilterOneTable[Product]*): Query[Product] = {
    services.productStore.findByCelebrity(id, filters: _*)
  }

  def productsInActiveInventoryBatches(): Seq[Product] = {
    this.activeProductsAndInventoryBatches.unzip._1
  }

  def activeProductsAndInventoryBatches: Seq[(Product, InventoryBatch)] = {
    services.productStore.findActiveProductsByCelebrity(id).toSeq
  }

  def secureInfo: Option[DecryptedCelebritySecureInfo] = {
    for {
      id <- secureInfoId
      secureInfo <- services.celebritySecureInfoStore.findById(id)
    } yield {
      secureInfo.decrypt
    }
  }

  /**
   * The orders sorted by most recently fulfilled.
   */
  def ordersRecentlyFulfilled: Iterable[FulfilledProductOrder] = {
    services.orderStore.findMostRecentlyFulfilledByCelebrity(this.id)
  }

  /**
   * If twitterUsername is not set, they might have a twitter account.
   * But if it is set to "none", we have checked, and they don't have one.
   */
  def doesNotHaveTwitter: Boolean = {
    twitterUsername.map(name => name.toLowerCase) == Some("none")
  }

  def hasTwitter: Boolean = {
    twitterUsername.isDefined && !doesNotHaveTwitter
  }

  def isAccountSettingsComplete: Boolean = {
    isAccountSettingsComplete(secureInfo)
  }

  // This method allows efficient reuse of code with CelebritySecureInfo
  private[models] def isAccountSettingsComplete(maybeSecureInfo: Option[CelebritySecureInfo]): Boolean = {
    maybeSecureInfo match {
      case None => false
      case Some(info) =>
        def numberOfContactMethods = info.numberOfContactMethods + (if (hasTwitter) 1 else 0)
        info.hasAllDepositInformation && (numberOfContactMethods >= 3)
    }
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
    //TODO: refactor profile image saving to here
    saveWithLandingPageImage(landingPageImage).saveWithLogoImage(logoImage)
  }

  private def saveWithLogoImage(logoImage: Option[BufferedImage]): Celebrity = {
    logoImage match {
      case None => this
      case Some(image) => {
        import ImageUtil.Conversions._

        val iconImageBytes = image.asByteArray(ImageAsset.Jpeg)
        withLogoImage(iconImageBytes).save().celebrity
      }
    }
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
   * @return whether this celebrity is in the MLB vertical, ie has a Category Value of name "MLB"
   */
  def isMlb: Boolean = {
    categoryValues.exists(_.name == "MLB")
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

  override def withGender(gender: Gender.EnumVal) = {
    this.copy(_gender = gender.name)
  }

  override lazy val defaultLandingPageImage = ImageAsset(
    IOUtils.toByteArray(current.resourceAsStream("images/1550x556.jpg").get),
    keyBase="defaults/celebrity",
    name="landingPageImage",
    imageType=ImageAsset.Jpeg,
    services=services.imageAssetServices.get
  )

  override def withLandingPageImageKey(key: Option[String]) : Celebrity = {
    this.copy(_landingPageImageKey = key)
  }

  override def imageAssetServices = services.imageAssetServices.get

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
  override def keyBase = {
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

  lazy val defaultLogoImage = ImageAsset(
    IOUtils.toByteArray(current.resourceAsStream("images/40x40.jpg").get),
    keyBase="defaults/celebrity",
    name="logoImage",
    imageType=ImageAsset.Png,
    services=services.imageAssetServices.get
  )
}

case class JsCelebrity(
  id: Long,
  publicName: String,
  enrollmentStatus: String,
  urlSlug: String,
  accountSettingsComplete: Boolean,
  created: Timestamp,
  updated: Timestamp
)

// TODO: After Play 2.1.1+ delete the extends FunctionX, for more info see https://groups.google.com/forum/#!topic/play-framework/ENlcpDzLZo8/discussion and https://groups.google.com/forum/?fromgroups=#!topic/play-framework/1u6IKEmSRqY
object JsCelebrity extends Function7[Long, String, String, String, Boolean, Timestamp, Timestamp, JsCelebrity] {
  import services.Time.ApiDateFormat
  implicit val celebrityFormats = Json.format[JsCelebrity]

  def from(celebrity: Celebrity): JsCelebrity = {
    JsCelebrity(
      id = celebrity.id,
      publicName = celebrity.publicName,
      enrollmentStatus = celebrity.enrollmentStatus.name,
      accountSettingsComplete = celebrity.isAccountSettingsComplete,
      urlSlug = celebrity.urlSlug,
      created = celebrity.created,
      updated = celebrity.updated
    )
  }
}

object Celebrity {
  val minProfileImageWidth = 100
  val minLogoWidth = 40
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

object CelebrityAccesskey {
  private val accesskeySalt = "tituspullo"

  /**
   * @param id the id of the celebrity accessible by accesskey
   * @return an accesskey that permits access to the celebrity regardless of published status
   */
  def accesskey(id: Long): String = {
    Base64.encodeBase64URLSafeString((id.toString + accesskeySalt).getBytes)
  }

  /**
   * @param accesskeyAttempt the attempted accesskey
   * @param id the id of the celebrity
   * @return whether the attempted accesskey matches the accesskey required by the celebrity matching the id
   */
  def matchesAccesskey(accesskeyAttempt: String, id: Long): Boolean = {
    accesskeyAttempt == accesskey(id)
  }

  /**
   * @param url the url to decorate with the accesskey
   * @param accesskey the accesskey
   * @return the url with the accesskey as a query parameter. If no accesskey was provided, then the original url is returned.
   */
  def urlWithAccesskey(url: String, accesskey: String): String = {
    if (accesskey.isEmpty) url else url + "?accesskey=" + accesskey
  }
}

class CelebrityStore @Inject() (
  schema: Schema,
  dbSession: DBSession,
  catalogStarsQuery: CatalogStarsQuery,
  celebrityCategoryValueStore: CelebrityCategoryValueStore
) extends SavesWithLongKey[Celebrity] with SavesCreatedUpdated[Celebrity] {

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
    if (slug.isEmpty) None
    else {
      from(schema.celebrities)(celebrity =>
        where(celebrity.urlSlug === slug)
          select (celebrity)).headOption
    }
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
   * Note: if you have a CategoryValue you can just use .celebrities to do the same.
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
   
  /**
   * Rebuilds the GiN index representing celebrities in the database. 
   * The index is used to make full text search fast.  
   * What is a GiN Index? It is this: http://www.postgresql.org/docs/9.0/static/textsearch-indexes.html
   * An intermediate step of this process is refreshing the materialized view that the GiN index references.
   * What is a materialized view? It is this: http://tech.jonathangardner.net/wiki/PostgreSQL/Materialized_Views
   **/
  def rebuildSearchIndex {
    // Drop the index
    SQL(
    """
        DROP INDEX celebrity_category_search_idx;
    """
        ).execute()(connection=schema.getTxnConnectionFactory)     
    // Refresh the materialized view
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

  private case class CelebrityProductSummary(inventoryAvailable: Int, minProductPrice: Int, maxProductPrice: Int)

  private def publishedSearch()
  : Iterable[(Celebrity, CelebrityProductSummary)] = {
    val queryString = """
    SELECT
     stuff2.celebrityid,
     stuff2.publicname,
     stuff2.roledescription,
     stuff2._landingpageImageKey,
     min(stuff2.minProductPrice) AS minProductPrice,
     max(stuff2.maxProductPrice) AS maxProductPrice,
     sum(stuff2.inventory_sold) AS inventory_sold,
     sum(stuff2.inventory_total) AS inventory_total,
     sum(stuff2.inventoryAvailable) AS inventoryAvailable
    FROM (
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
     CASE WHEN ib.startdate < now() AND ib.enddate > now() THEN ib.numInventory
          ELSE 0
     END AS inventory_total
    FROM
     celebrity c INNER JOIN product p ON (p.celebrityid = c.id)
                 INNER JOIN inventorybatch ib ON (ib.celebrityid = c.id)
                 LEFT OUTER JOIN orders o ON (o.inventorybatchid = ib.id AND ib.startdate < now() AND ib.enddate > now() AND
                                              o.productid = p.id)
    WHERE
     c._publishedStatus = 'Published' AND
     p._publishedStatus = 'Published'
    ORDER BY is_order ASC
    ) AS stuff
      GROUP BY celeb_id, celeb_publicname, celeb_roledescription, celeb_landingpageImageKey, inventorybatch_id, inventory_total
    ) AS stuff2
    GROUP BY celebrityid, publicname, roledescription, _landingpageImageKey
    """

    val rowStream = SQL(queryString).apply()(connection = schema.getTxnConnectionFactory)
    for (row <- rowStream) yield {
      import java.math.BigDecimal
      val celebrity = Celebrity(
        id = row[Long]("celebrityid"),
        publicName = row[String]("publicname"),
        roleDescription = row[String]("roledescription"),
        _enrollmentStatus = EnrollmentStatus.Enrolled.name,
        _publishedStatus = PublishedStatus.Published.name,
        _landingPageImageKey = row[Option[String]]("_landingpageImageKey")
      )
      val productSummary = CelebrityProductSummary(
        row[BigDecimal]("inventoryAvailable").intValue(),
        row[BigDecimal]("minProductPrice").intValue(),
        row[BigDecimal]("maxProductPrice").intValue())
      (celebrity, productSummary)
    }
  }

  def celebritiesSearch(maybeQuery: Option[String] = None, refinements: Iterable[Iterable[Long]] = Iterable[Iterable[Long]]())
  : Iterable[Long] = {
    // Note we could make this fast probably.  We should see how performance is affected if we don't have to
    // join across all celebrities since we can filter some with the text search first.
    val queryString = """
    SELECT
     c.id AS celebrityid
    FROM
     celebrity c 
    """ +
     (
       if(maybeQuery.isDefined)
"""             INNER JOIN celebrity_categories_mv mv ON (mv.id = c.id) """
       else " "
     )

    val queryTextMatching = """ mv.to_tsvector @@ plainto_tsquery('english', {textQuery}) """

    val queryRefinementsParts = for {
      refinementList <- refinements
    } yield {
      """ EXISTS ( SELECT c.id FROM celebritycategoryvalue ccv WHERE ccv.categoryvalueid IN """ +
      refinementList.mkString("(", ",", ")") + """ AND ccv.celebrityid = c.id ) """
    }

    val queryRefinements = queryRefinementsParts.mkString(" AND ")

    val unfilteredWhereParts = (maybeQuery, queryTextMatching) :: (refinements, queryRefinements) :: Nil

    def isDefined(condition: AnyRef): Boolean = {
      condition match {
        case iterable: Iterable[_] => !iterable.isEmpty
        case Some(something) => true
        case _ => false
      }
    }

    val whereParts = for {
      (condition, wherePart) <- unfilteredWhereParts
      if (isDefined(condition))
    } yield {
      wherePart
    }

    val queryWhere = " WHERE " + (if (whereParts.isEmpty) {
      " 1 = 1 "
    } else {
      whereParts.mkString(" AND ")
    })

    val unboundQuery = SQL(queryString + queryWhere)
    val finalQuery = maybeQuery match {
      case None => unboundQuery
      case Some(query) => unboundQuery.on("textQuery" -> query)
    }

    finalQuery.apply()(connection = schema.getTxnConnectionFactory).map(row => row[Long]("celebrityid")).toIndexedSeq
  }

  /**
   * This function is a dupe of the above function. We will probably want to think of a good way to abstract the search if we want to retain a
   * useful admin text search. 
   * 
   * Currently the view conversion is doing another DB query to fill in that data for each celebrity, would be great if we could do it all in one trip. 
   *
   * @param maybeQuery the text to search for. This will be tested against the celebrity's name, category values,
   *                   descriptions, and a few other sources.
   * @param refinements this kind of sucks, but it's a way of communicating AND and OR combinations of different
   *                    category values for the queried celebrities. Elements of the nested Iterable[Long]s will be interpreted
   *                    as OR relationships, and each outer element will be compared to its siblings with AND
   *                    relationships. For example, imagine you wanted to search for PITCHERS(categoryvalueid=1) named
   *                    "Derpson" on the Yankees(cvid=2) or the Mets(cvid=3), the conceptual query
   *                    you want is the set of celebrities named Derpson with "PITCHERS and (METS or YANKEES)".
   *                    You would phrase the query against marketplaceSearch as:
   *                    marketplaceSearch(Some("Derpson"), List(List(1), List(2, 3))
   */
  def marketplaceSearch(maybeQuery: Option[String] = None, refinements: Iterable[Iterable[Long]] = Iterable[Iterable[Long]]())
  : Iterable[MarketplaceCelebrity] = {

    for {
      star <- catalogStarsSearch(maybeQuery, refinements)
    } yield {
      MarketplaceCelebrity.make(
        id = star.id,
        publicName = star.name,
        photoUrl = star.marketplaceImageUrl,
        storefrontUrl = star.storefrontUrl,
        inventoryRemaining = star.inventoryRemaining,
        minPrice = star.minPrice,
        maxPrice = star.maxPrice,
        secondaryText = star.secondaryText
      )
    }
  }

  def catalogStarsSearch(maybeQuery: Option[String] = None, refinements: Iterable[Iterable[Long]] = Iterable[Iterable[Long]]())
  : Iterable[CatalogStar] = {

    val catalogStars = catalogStarsQuery()
    val catalogStarsById = catalogStars.groupBy(star => star.id)

    for {
      celebrityId <- celebritiesSearch(maybeQuery, refinements)
      if(catalogStarsById.contains(celebrityId))
    } yield {
      catalogStarsById(celebrityId).head
    }
  }

  def getCatalogStars: Iterable[CatalogStar] = {
    val celebrityAndSummaries = publishedSearch()
    val catalogStarFutures = for (celebrityAndSummary <- celebrityAndSummaries) yield {
      Akka.future {
        val (celebrity, summary) = celebrityAndSummary
        celebrity.asCatalogStar(
          inventoryRemaining = summary.inventoryAvailable,
          minPrice = summary.minProductPrice,
          maxPrice = summary.maxProductPrice
        )
      }
    }

    Promise.sequence(catalogStarFutures).await(DateTimeConstants.MILLIS_PER_MINUTE).get.toIndexedSeq
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

  /**
   * Update a celebrity's associated filter values
   **/
  def updateCategoryValues(celebrity: Celebrity, categoryValueIds: Iterable[Long]) {
    //remove old records
    celebrity.categoryValues.dissociateAll

    // Add records for the new values
    val newCelebrityCategoryValues = for (categoryValueId <- categoryValueIds) yield {
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

  //
  // SavesCreatedUpdated[Celebrity] methods
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
