package models

import enums.PublishedStatus.EnumVal
import enums.{PublishedStatus, HasPublishedStatus}
import frontend.landing.CatalogStar
import java.sql.Timestamp
import services.db.{FilterOneTable, Schema, SavesWithLongKey, KeyedCaseClass}
import org.apache.commons.io.IOUtils
import org.joda.money.Money
import services.Finance.TypeConversions._
import models.Product.ProductWithPhoto
import services.blobs.Blobs.Conversions._
import java.awt.image.BufferedImage
import play.api.Play
import services.blobs.AccessPolicy
import com.google.inject.{Provider, Inject}
import services._
import mvc.celebrity.CelebrityViewConversions
import org.squeryl.Query
import play.api.Play.current
import graphics.GraphicsSource
import org.squeryl.dsl.{GroupWithMeasures, ManyToMany}
import java.util.Date
import ImageUtil.Conversions._

case class ProductServices @Inject() (
  store: ProductStore,
  celebStore: CelebrityStore,
  orderStore: OrderStore,
  inventoryBatchStore: InventoryBatchStore,
  imageAssetServices: Provider[ImageAssetServices],
  graphicsSourceFactory: () => GraphicsSource
)

/**
 * An item on sale by a Celebrity. In the case of the base Egraph, it represents a signature service
 * against a particular photograph of the celebrity.
 */
case class Product(
  id: Long = 0L,
  celebrityId: Long = 0L,
  priceInCurrency: BigDecimal = Product.defaultPrice,
  name: String = "",
  description: String = "",
  _defaultFrameName: String = PortraitEgraphFrame.name,
  storyTitle: String = "The Story",
  storyText: String = "(No story has been written for this product)",
  signingScaleW: Int = 0,
  signingScaleH: Int = 0,
  signingOriginX: Int = 0,
  signingOriginY: Int = 0,
  signingAreaW: Int = Product.defaultSigningAreaW,
  signingAreaH: Int = Product.defaultSigningAreaW,
  photoKey: Option[String] = None, // todo: rename to _photoKey
  _iconKey: Option[String] = None,
  _publishedStatus: String = PublishedStatus.Unpublished.name,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: ProductServices = AppConfig.instance[ProductServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated with HasPublishedStatus[Product] {

  lazy val inventoryBatches = services.inventoryBatchStore.inventoryBatches(this)

  //
  // Additional DB columns
  //
  /** The slug used to access this product from the main site */
  val urlSlug = Product.slugify(name)

  //
  // Public members
  //
  def save(): Product = {
    require(!name.isEmpty, "Product: name must be specified")
    require(!description.isEmpty, "Product: description must be specified")
    require(!storyTitle.isEmpty, "Product: storyTitle must be specified")
    require(!storyText.isEmpty, "Product: storyText must be specified")
    services.store.save(this)
  }

  /**
   * Saves product with image and icon. If image is set, then signingScaleW and signingScaleH are also set to default values
   * depending on the image.
   */
  def saveWithImageAssets(image: Option[BufferedImage], icon: Option[BufferedImage]): Product = {
    

    // Prepare the product photo, cropped to the suggested frame
    val product = image match {
      case None => save()
      case Some(img) => {
        val imageDimensions = Dimensions(img.getWidth, img.getHeight)
        val frame = EgraphFrame.suggestedFrame(imageDimensions)
        val imageCroppedToFrame = frame.cropImageForFrame(img)
        val imageByteArray = imageCroppedToFrame.asByteArray(ImageAsset.Jpeg)
        // Product has previously been saved so that it has an ID for blobstore to key on
        val savedWithFrame = withFrame(frame).save()

        val signingScaleDimensions = if (imageDimensions.isLandscape) Product.defaultLandscapeSigningScale else Product.defaultPortraitSigningScale
        savedWithFrame.copy(signingScaleH = signingScaleDimensions.height, signingScaleW = signingScaleDimensions.width).withPhoto(imageByteArray).save().product
      }
    }

    // Prepare the product plaque icon, cropped to a square
    icon match {
      case None =>
      case Some(ic) => {
        val iconCroppedToSquare = ImageUtil.cropToSquare(ic)
        val iconBytes = iconCroppedToSquare.asByteArray(ImageAsset.Jpeg)
        product.withIcon(iconBytes).save()
      }
    }

    product
  }

  def iconUrl: String = {
    icon.url
  }

  /**
   * Returns a copy of the Product with a different frame
   *
   * @param frame the EgraphFrame that will be used to frame this type of
   *   egraph by default. This should be based on the dimensions of the
   *   image.
   */
  def withFrame(frame: EgraphFrame): Product = {
    copy(_defaultFrameName=frame.name)
  }

  /**
   * Returns the default Frame for this Product. This is the frame that will
   * be used to frame egraphs made on top of this product by default.
   **/
  def frame: EgraphFrame = {
    _defaultFrameName match {
      case PortraitEgraphFrame.name => PortraitEgraphFrame
      case LandscapeEgraphFrame.name => LandscapeEgraphFrame
    }
  }

  def icon: ImageAsset = {
    val imageAssetOption = _iconKey.map { theKey =>
      ImageAsset(keyBase, theKey, ImageAsset.Png, services=services.imageAssetServices.get())
    }

    imageAssetOption.getOrElse(defaultIcon)
  }

  private def withIcon(iconImageData:Array[Byte]): ProductWithPhoto = {
    val newIconKey = "icon_" + Time.toBlobstoreFormat(Time.now)

    ProductWithPhoto(
      product=this.copy(_iconKey=Some(newIconKey)),
      photo=ImageAsset(iconImageData, keyBase, newIconKey, ImageAsset.Png, services.imageAssetServices.get)
    )
  }

  private def withPhoto(imageData:Array[Byte]): ProductWithPhoto = {
    val newPhotoKey = Time.toBlobstoreFormat(Time.now)

    ProductWithPhoto(
      product=this.copy(photoKey=Some(newPhotoKey)),
      photo=ImageAsset(imageData, keyBase, newPhotoKey, ImageAsset.Jpeg, services.imageAssetServices.get)
    )
  }

  def photo: ImageAsset = {
    photoKey.flatMap(theKey => Some(ImageAsset(keyBase, theKey, ImageAsset.Jpeg, services=services.imageAssetServices.get))) match {
      case Some(imageAsset) =>
        imageAsset

      case None =>
        defaultPhoto
    }
  }

  def photoAtPurchasePreviewSize: ImageAsset = {
    photo.resizedWidth(frame.purchasePreviewWidth)
  }

// This is pretty crappy. This would be much better done on the browser-side. Left this disabled.
  // Enable it by using productPhotoPreview instead of productPhoto on admin_celebrityproductdetail.scala.html.
  def signingAreaPreview(width: Int = 600): String = {
    import graphics.{Handwriting, HandwritingPen}
    val _photo = photo
    val ingredients = () => {
      EgraphImageIngredients(
        signatureJson = Product.signingAreaSignatureStr,
        messageJsonOption = Some(Product.signingAreaMessageStr),
        pen = HandwritingPen(width = Handwriting.defaultPenWidth),
        photo = _photo.renderFromMaster,
        photoDimensionsWhenSigned = Dimensions(signingScaleW, signingScaleH),
        signingOriginX = signingOriginX,
        signingOriginY = signingOriginY
      )
    }

    val signingAreaPreviewImage = EgraphImage(
      ingredientFactory = ingredients, 
      graphicsSource = services.graphicsSourceFactory(),
      blobPath = _photo.key
    )
    val rendered = signingAreaPreviewImage
      .withSigningOriginOffset(signingOriginX.toDouble, signingOriginY.toDouble)
      .scaledToWidth(width)
      .transformAndRender
    val bytes = rendered.graphicsSource.asByteArray
    val blobs = rendered.services.blobs
    val blobKey = keyBase + "/signingareapreview.svgz"
    blobs.put(blobKey, bytes, AccessPolicy.Public)
    blobs.getUrlOption(blobKey).get
  }

  def signingScalePhoto: ImageAsset = {
    photo.resizedWidth(signingScaleW).getSaved(AccessPolicy.Public)
  }

  def photoImage: BufferedImage = {
    photo.renderFromMaster
  }

  def price: Money = {
    priceInCurrency.toMoney()
  }

  def withPrice(money: Money) = {
    copy(priceInCurrency=BigDecimal(money.getAmount))
  }

  def withPrice(money: BigDecimal) = {
    copy(priceInCurrency=money)
  }

  def renderedForApi: Map[String, Any] = {
    renderCreatedUpdatedForApi ++ Map(
      "id" -> id,
      "urlSlug" -> urlSlug,
      "photoUrl" -> signingScalePhoto.url,
      "iPadSigningPhotoUrl" -> signingScalePhoto.url,
      "signingScaleW" -> signingScaleW,
      "signingScaleH" -> signingScaleH,
      "signingOriginX" -> signingOriginX,
      "signingOriginY" -> signingOriginY,
      "signingAreaW" -> signingAreaW,
      "signingAreaH" -> signingAreaH
    )
  }

  /** Retrieves the celebrity from the database */
  def celebrity: Celebrity = {
    services.celebStore.get(celebrityId)
  }

  //TODO: Create Jira and link here. This should be improved to be 1 query.
  /**
   * Returns the remaining inventory of this Product and the active InventoryBatches for this Product.
   *
   * wchan: I have to admit that I don't remember why I wrote it this way. But the InventoryBatches are used when
   * created an Order against a specific InventoryBatch (this way, the InventoryBatch will already be in memory).
   */
  def getRemainingInventoryAndActiveInventoryBatches(): (Int, Seq[InventoryBatch]) = {
    val activeInventoryBatches = services.inventoryBatchStore.getActiveInventoryBatches(this).toSeq
    val accumulatedInventoryCountAndBatchId = (0, List.empty[Long])
    val (totalInventory, inventoryBatchIds) = activeInventoryBatches.foldLeft(accumulatedInventoryCountAndBatchId) { (accum, inventoryBatch) =>
      val (currentInventoryCount, currentBatchIdList) = accum
      (currentInventoryCount + inventoryBatch.numInventory, inventoryBatch.id :: currentBatchIdList)
    }
    val numOrders = services.orderStore.countOrders(inventoryBatchIds)
    (totalInventory - numOrders, activeInventoryBatches)
  }

  /**
   * Returns the most appropriate inventory batch to purchase from on this product, if there
   * were any active inventory batches.
   *
   * The most appropriate is the one that will get egraph to the customer fastest, which is
   * the inventory batch that ends the soonest.
   */
  // TODO: SER-656 fix this, it is broken.  It isn't even checking to make sure there is inventory available.
  def nextInventoryBatchToEnd: Option[InventoryBatch] = {
    val batches = services.inventoryBatchStore.getActiveInventoryBatches(this).toSeq

    batches.sortWith((batch1, batch2) => batch1.endDate.before(batch2.endDate)).headOption
  }

  /** Returns the remaining inventory in this product's batch */
  def remainingInventoryCount: Int = {
    this.getRemainingInventoryAndActiveInventoryBatches()._1
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = {
    Product.unapply(this)
  }

  //
  // HasPublishedStatus[Product] methods
  //
  override def withPublishedStatus(status: EnumVal) = {
    this.copy(_publishedStatus = status.name)
  }

  //
  // Private members
  //
  private def keyBase = {
    require(id > 0, "Can not determine blobstore key when no id exists yet for this entity in the relational database")

    "product/" + id
  }

  lazy val defaultPhoto = ImageAsset(
      new BufferedImage(2160, 1440, BufferedImage.TYPE_INT_RGB).asByteArray(ImageAsset.Jpeg),
      keyBase="defaults/product",
      name="photo",
      imageType=ImageAsset.Jpeg,
      services=services.imageAssetServices.get
  ).getSaved(AccessPolicy.Public)

  lazy val defaultPhotoPortrait = ImageAsset(
    new BufferedImage(1440, 2160, BufferedImage.TYPE_INT_RGB).asByteArray(ImageAsset.Jpeg),
    keyBase="defaults/product",
    name="photo",
    imageType=ImageAsset.Jpeg,
    services=services.imageAssetServices.get
  ).getSaved(AccessPolicy.Public)

  lazy val defaultIcon = ImageAsset(
    IOUtils.toByteArray(current.resourceAsStream("images/egraph_default_plaque_icon.png").get),
    keyBase="defaults/product",
    name="photo_icon",
    imageType=ImageAsset.Png,
    services=services.imageAssetServices.get
  ).getSaved(AccessPolicy.Public)
}

object Product {

  val defaultPrice = BigDecimal(50)
  val defaultSigningAreaW = 1024 // This is the width of an iPad2 screen. Default signing area is 1024x1024.
  val defaultLandscapeSigningScale = Dimensions(width = 1615, height = 1024)
  val defaultPortraitSigningScale = Dimensions(width = 1024, height = 1428)
  val minPhotoWidth = 1024
  val minPhotoHeight = 1024
  val minIconWidth = 40

  lazy val signingAreaMessageStr = "{\"x\":[[57,49,46,44,41,39,36,33,31,29,26,22,19,17,14,12,11,10,8,7,7,7,6,6,6,6,6,6,6,6,6,6,6,7,7,7,7,8,8,9,9,9,9,9,9,10,10,11,11,11,11,11,11,12,12,12,12,12,12,12,12,12,12,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,12,12,12,12,12,12,12,12,11,11,11,11,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,9,9,8,8,8,8,8,7,7,7,7,7,7,7,7,7,7,7,7,7,6,6,5,2],[52,61,63,66,69,72,75,79,82,85,89,92,95,99,101,105,109,113,116,120,124,129,133,138,142,146,151,156,161,166,170,175,180,186,190,195,200,205,210,216,221,226,232,239,245,250,257,265,271,278,285,293,300,308,317,323,330,338,345,352,358,365,372,377,383,391,397,403,409,416,422,435,442,450,457,465,474,480,488,495,503,510,518,525,532,540,547,553,561,567,575,582,589,594,601,614,620,627,634,639,644,651,658,663,668,674,680,686,693,698,704,711,716,722,728,741,747,754,761,768,774,781,788,795,800,805,812,817,823,828,834,839,843,848,853,858,862,867,871,876,883,887,892,896,900,903,907,911,915,919,923,926,930,934,937,941,945,948,952,955,959,962,965,969,975,979,982,985,989,992,995,999,1003,1006,1009,1011,1012,1014,1015,1016,1017,1018,1018,1019,1020,1021,1021,1021,1021,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1022,1021,1021,1021,1020,1020,1020,1020,1019,1019,1019,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1017,1017,1017,1017,1017,1017,1017,1017,1017,1017,1017,1017,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1016,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1015]],\"y\":[[19,13,13,12,12,9,8,7,6,5,4,3,3,3,3,3,3,3,3,5,7,9,12,15,19,22,27,31,34,38,42,45,47,50,51,54,56,58,61,64,66,70,73,75,80,83,85,90,94,97,101,105,107,111,116,120,124,129,133,138,143,148,152,157,161,167,172,181,186,191,196,201,206,211,216,221,227,232,236,243,249,254,260,264,271,277,284,289,295,304,309,322,330,335,342,347,355,360,366,371,378,383,389,393,400,406,411,416,422,429,435,440,445,453,460,464,478,485,490,496,503,510,516,523,528,537,541,547,552,559,566,571,576,581,587,592,598,603,610,620,625,630,636,641,647,651,657,663,669,673,677,679,681,683,685,687,692,693,695,696,698],[10,4,4,4,3,3,3,3,3,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,3,3,3,3,3,4,4,5,5,6,6,6,6,7,7,7,7,7,7,7,7,7,7,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,7,6,6,5,5,5,4,4,4,4,4,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,4,4,4,4,4,4,4,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,4,4,4,4,3,3,3,4,5,6,8,10,12,13,16,18,21,22,24,27,28,32,35,38,42,45,47,51,53,56,59,62,66,70,73,76,77,80,82,85,88,91,95,98,100,103,105,107,110,114,117,120,124,127,129,132,134,138,142,145,152,155,158,161,164,168,173,177,179,183,185,188,192,197,201,204,207,211,214,218,222,226,231,237,241,246,251,254,258,261,265,269,274,279,283,287,290,296,301,307,311,315,321,327,332,337,341,352,357,362,366,371,376,382,386,390,395,401,406,411,415,419,424,430,435,440,444,449,460,464,469,473,478,484,489,494,498,503,508,513,518,522,526,531,537,542,547,552,557,564,569,574,579,592,596,601,606,612,617,622,626,630,636,641,646,649,653,656,659,664,668,671,674,677,678,680,681,683,685,687,688,690,693,695,696,698]],\"t\":[[0,124472417,140800417,156364709,172587334,190634417,205002917,220453750,236781084,252297292,268332167,284712709,300226709,316552709,333076875,348638375,364391709,380829542,396362542,412490625,428894459,446053125,462102625,478566042,494634834,511690417,527887417,545010959,560860167,578236709,594399084,611502792,627357792,644793959,661327209,677149417,694204042,711498459,727495750,744967417,761095584,778203459,793945125,811030917,828791792,845381167,862246042,878016209,894571792,911690750,927790417,943889417,961258459,978343667,994232542,1011445500,1027276042,1044449375,1061979000,1077915500,1093935667,1111253959,1127245542,1144600584,1161546792,1177289750,1194430042,1212190125,1229014667,1245344084,1261941209,1278089500,1293940125,1310883375,1328259209,1344122417,1361266042,1378289667,1394594792,1410483084,1427453834,1444856334,1460677042,1477903917,1493950834,1511041334,1528093709,1543995084,1561164584,1578310959,1594196959,1612072709,1628769417,1645360167,1661626084,1677708834,1694823834,1711001209,1728307917,1744169750,1761520959,1777398375,1794581125,1810494292,1827485625,1844839542,1860669584,1878273334,1894238709,1911597792,1927802459,1944947167,1960707875,1977902125,1995463084,2012016709,2028893917,2045416875,2062037459,2079028792,2094878542,2110919000,2128375292,2144207917,2161434000,2177382334,2194846250,2210597959,2227876417,2244997875,2260788042,2278199000,2294152417,2311439667,2327349167,2344292834,2361967000,2378876875,2396099000,2413083792,2428317667,2444590375,2460920959,2477337417,2494666417,2510716167,2528159500,2543912750,2560982959,2578151584,2594035125,2611273459,2627122459,2644207417,2661472000,2677241709,2694435375,2710474667,2727663209,2745004417,2760542459],[3965991875,4107179750,4123454125,4139131084,4155287792,4171703209,4187249250,4203049667,4219453750,4235082042,4250995917,4267304584,4282756875,3889288,20222246,35879454,51777704,68202746,84241079,100613204,117261038,133925204,150664788,164756454,179970496,196122996,212585996,227786996,243829038,260168329,275735871,291815538,308139329,323718579,339722371,356157996,371703454,387841079,404281663,419821621,435806704,452332829,467871538,483928204,500149746,516903871,533590788,550011246,565464871,582295788,599097913,617197579,632977538,648711454,665600704,682350788,699432413,716350163,732204454,749127496,765991079,782930829,798850121,815915204,832867913,849798371,865464746,882087288,899396871,916622246,932752079,949411829,966446079,983169704,998880871,1015478204,1032066538,1048766538,1065447996,1082108371,1098779746,1115519954,1132208746,1149130913,1165987163,1182916663,1199867579,1215353996,1232603454,1249765121,1265582829,1282300704,1300262163,1316668704,1332399871,1349164204,1366103954,1383068746,1399899288,1415459788,1432219704,1448985038,1465653413,1482505496,1499394913,1516317829,1533107746,1550216329,1565912079,1583130413,1598952496,1615732954,1632456329,1649219496,1666361871,1683630329,1700154913,1716838288,1733568746,1750238454,1766026079,1782980954,1799730204,1816481413,1833171746,1848662204,1865470954,1882172163,1898795246,1915504663,1932123288,1948883538,1965823496,1982957496,1999829788,2016515371,2032005996,2048738371,2065492454,2082315163,2100241788,2116934121,2133556788,2149015246,2165746704,2182707579,2198667288,2216493371,2233286121,2248878663,2265705871,2282613288,2299693288,2316466079,2332011788,2348770413,2365554871,2382276329,2399786579,2416489329,2433176621,2448678121,2466532704,2482703621,2500129663,2516936913,2533556246,2549244913,2565959413,2582990996,2599936038,2615525121,2632343038,2649183121,2665918746,2682681829,2699868079,2715450871,2732287288,2748941413,2765652621,2782352079,2799130579,2816451996,2832182496,2867346704,2883919579,2900819246,2917266163,2931031996,2947607913,2963259413,2979162579,2995290621,3010826996,3026823871,3043262246,3058828746,3076134246,3108573246,3110203329,3123417663,3139063704,3154962829,3171262288,3187671538,3203061288,3219086996,3235452413,3251084413,3267213746,3283533163,3300259288,3316828246,3332617288,3349561871,3366358038,3382372454,3398997079,3415632246,3432432913,3449080871,3466306454,3483676913,3499204288,3516204079,3533137163,3548667454,3565431663,3582282496,3599008246,3615990246,3633066621,3649856246,3665313329,3682749329,3700080496,3715813121,3732434371,3749353663,3766264871,3783078829,3799771788,3816502829,3832105621,3848851704,3865454954,3882309871,3899185621,3915982288,3933126079,3948722621,3965421038,3982146871,3999181246,4016331079,4033180996,4048744163,4065355621,4083288246,4099411163,4116917538,4132466246,4149401163,4166320663,4183131329,4198784746,4215626038,4232406621,4249162829,4265948746,4282873913,4705408,20442950,37091450,53714867,70374867,87187117,104255825,121260075,138033575,155125325,171604658,187726283,204209242,221057950,237959408,254727950,271528283,288333950,305449117,321106992,338275325,353762950,370554575,387168492,403796533,420514658,437627908,454446992,471481742,488289492,503871950,520715700,537478700,554241117,571953325,588624950,605284325,621844992,637270950,653998575,670876492,688072950,704084117,720796617,737484992,754383200,771174825,788046200,804737742,821512117,837296700,854062283,870667158,887680783,904448825,921227117,938215158,954001825,971556533,988636158,1005211992,1020772450,1037615408,1054338867,1071309408,1087066533,1103839200,1120672742,1137367450,1154021617,1170609783,1187346283,1204133117,1220855075,1237775075,1254686658,1271500075,1288748783,1304000075,1320980158,1338169200,1354319992,1371083283,1388605450,1405461075,1421008408,1436993408,1454925533,1470586033,1487231742,1504053367,1520815992]]}"
  lazy val signingAreaSignatureStr = "{\"x\":[[1017,1016,1016,1016,1016,1016,1016,1016,1017,1017,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1018,1017,1015,1015,1015,1015,1015,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1015,1015,1015,1015,1015,1015,1015,1015,1015,1015,1014,1014,1014,1013,1012,1009,1007,1004,999,994,989,985,981,977,973,968,964,960,955,950,946,941,936,932,927,923,918,914,909,904,898,894,888,883,877,872,867,860,853,848,841,834,827,819,811,804,797,790,782,775,768,753,746,738,729,723,715,707,700,693,687,679,672,665,659,652,644,637,630,623,616,610,603,596,591,584,577,571,559,553,547,541,534,527,522,514,508,501,494,489,481,474,467,460,453,445,440,434,427,420,416,411,401,395,390,384,377,372,366,360,354,349,343,338,333,328,324,319,314,310,306,301,297,292,287,283,275,270,265,260,256,251,247,243,238,234,229,225,221,216,211,207,203,199,195,191,186,177,173,169,164,158,153,148,144,139,134,131,127,123,119,116,113,110,106,102,99,95,90,86,78,74,70,66,62,58,54,50,46,43,39,36,32,29,24,21,17,15,13,12,10,9,8,8,8,8,8,8,7,7,7,7,7,7,7,8,8,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,9,8,8,8,8,8,8,8,9,9,10,11,11,11,12,12,12,12,12,12,12,12,12,12,12,12,12,12,12,12,11,10,8]],\"y\":[[707,715,719,724,728,731,735,738,741,746,750,754,758,762,765,770,775,779,783,786,789,794,799,803,807,811,815,818,822,827,832,835,840,844,847,852,856,860,863,867,873,877,882,887,891,894,898,901,906,911,915,919,922,926,930,935,940,943,946,950,952,956,961,964,969,971,974,979,982,985,989,992,994,997,998,999,1000,1001,1002,1003,1005,1006,1007,1008,1009,1010,1010,1010,1011,1011,1011,1011,1013,1014,1014,1015,1015,1015,1015,1015,1015,1015,1015,1015,1014,1013,1013,1013,1013,1012,1011,1011,1010,1010,1009,1009,1008,1008,1008,1008,1008,1007,1007,1007,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1006,1007,1007,1007,1007,1007,1007,1007,1007,1007,1008,1008,1008,1008,1008,1008,1008,1009,1009,1009,1009,1010,1010,1011,1011,1011,1011,1012,1012,1012,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1013,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1014,1013,1013,1013,1013,1013,1012,1012,1012,1012,1012,1013,1013,1012,1011,1010,1008,1006,1004,1003,1001,999,996,991,987,983,979,976,974,971,968,966,963,959,956,954,952,950,947,945,941,938,934,930,926,923,920,916,911,906,902,899,896,891,885,880,875,871,865,858,853,848,836,831,825,819,812,803,798,791,783,775,770,765,759,752,747,742,738,733,727,721,717,713,709,704,701,697,691,687,684,680,678]],\"t\":[[3845565904,3986642821,4003072279,4018540821,4038566196,4051097404,4066374904,4082536654,4098971654,4114950904,4130763529,4147409904,4162577487,4178554529,4195012321,4210674029,4227009529,4243699862,4260114487,4275756946,4292659862,14516566,31342566,48303525,64006191,80627816,97435233,114166275,130977150,147933233,163605108,181901566,198798775,215129233,231842441,247589525,264259566,281111108,298252191,314002816,332058441,348690566,365116400,381948358,397622816,414438275,431413733,447263150,463998816,480686191,497346108,514137858,530721650,547714191,564525025,581350025,598206400,613832191,630519983,648112400,664891191,680342608,697198441,713798733,731505691,748697775,764395483,780575483,797464066,814124108,830822983,847681941,864499733,881355275,898200733,913711150,931538816,947116775,963963608,986960650,1018907483,1034940108,1051387775,1066937775,1099466733,1115780483,1132329275,1149359150,1165772983,1179254733,1195496108,1211215691,1227251400,1243379316,1258883983,1274887108,1291366816,1306894650,1322849108,1339289275,1354836441,1370861650,1387285566,1403229775,1419243358,1435841483,1451024816,1467052691,1483430983,1499086858,1515349525,1531950941,1548607191,1564192775,1581032150,1597994066,1614890566,1630588066,1647322233,1663950108,1680566816,1697211108,1713963358,1730558816,1747627858,1764711358,1780415900,1797168941,1813821400,1830590941,1847530775,1864787066,1880568483,1897337775,1915349066,1932031316,1948686483,1965317983,1980947233,1997898441,2014896941,2030454900,2047352858,2064029733,2080799983,2097694233,2114593691,2131415775,2148150566,2163746691,2180499775,2197140025,2213838441,2230914566,2247632816,2264707400,2280551566,2297298441,2315135983,2331537483,2347721150,2363958691,2380729358,2397551608,2414472691,2431368150,2448109483,2464861816,2480615275,2497227483,2513953108,2530718191,2547338900,2563944191,2580804983,2597608525,2614830358,2630746191,2647989525,2664901900,2680723316,2698297233,2714329483,2731137108,2747298900,2764152608,2781209900,2798108858,2813897608,2830609400,2847260566,2864088316,2881222025,2898090775,2914837525,2930451608,2947112858,2963776941,2980544650,2997163316,3013972316,3031169108,3047987150,3064891941,3080635275,3098388316,3114461775,3130920358,3147138233,3164123275,3181012566,3197980650,3214880275,3230394733,3247181025,3263888358,3280616566,3297539858,3314333191,3331137816,3348032483,3363763525,3380613733,3397336316,3414091733,3431121191,3447916316,3464783900,3480477025,3497258691,3515369608,3531876233,3548437858,3563979733,3580735941,3597479358,3614995483,3630700566,3647427983,3664158525,3681511400,3697056858,3714879900,3730395941,3747062691,3763732900,3780490733,3797552900,3814839566,3831563066,3847159191,3863942483,3880945691,3898863650,3915546566,3932263941,3948247566,3964016316,3980881150,3998306191,4014355025,4031747816,4047533025,4064700816,4080632775,4097994358,4114033400,4131083316,4148298316,4164251108,4181338233,4197408691,4214313608,4231628900,4247363775,4266630525,4229437,20897312,37534229,51576187,67953270,83508979,99282062,115535520,131136020,147093354,163586229,179058437,195166312,211508645,227252520,243035854,259457479,275149562,291401020,307780687,323252354,339222770,355703729,371223437,387400062,403904020,420624645,436969062,455226895,470100104,486204687,503451770,519541687,536874395,553180687,569122604,586392979,602307270,619286770,636775812,652618062,669901437,685941062,703381729,719357687,736584270,752603437,770063645,785910687,803747729,820583729,837306729,852256687,869412520,886822770,904080770,929140687,936915729,954159854,970220937,986561729,1002525229,1019767604,1036827520,1052629062,1069810395,1086824812,1102670312,1120142229,1135881395,1153017562,1168945104,1186878270,1203626604,1220449145,1236544187,1252351895,1283414895,1299003604,1314906520]]}"

  def slugify(productName: String): String = {
    Utils.slugify(productName, false) // Slugify without lower-casing
  }

  case class ProductWithPhoto(product: Product, photo: ImageAsset) {
    def save(): ProductWithPhoto = {
      val savedPhoto = photo.save(AccessPolicy.Public)
      val savedProduct = product.save()

      ProductWithPhoto(savedProduct, savedPhoto)
    }
  }
}

class ProductStore @Inject() (schema: Schema, inventoryBatchQueryFilters: InventoryBatchQueryFilters) extends SavesWithLongKey[Product] with SavesCreatedUpdated[Long,Product] {
  import org.squeryl.PrimitiveTypeMode._

  //
  // Public members
  //

  def findActiveProductsByCelebrity(celebrityId: Long): Query[Product] = {
    from(schema.products, schema.inventoryBatchProducts, schema.inventoryBatches)((product, association, inventoryBatch) =>
      where(
        product.celebrityId === celebrityId
          and association.productId === product.id
          and association.inventoryBatchId === inventoryBatch.id
          and (Time.today between(inventoryBatch.startDate, inventoryBatch.endDate))
      )
        select (product)
    )
  }

  def getProductAndInventoryBatchAssociations(inventoryBatchIds: Seq[Long]) = {
    from(schema.products, schema.inventoryBatchProducts, schema.inventoryBatches)((product, association, inventoryBatch) =>
      where(association.productId === product.id and (association.inventoryBatchId in inventoryBatchIds))
        select ((product, association.inventoryBatchId))
    )
  }

  /** Locates all of the products being sold by a particular celebrity */
  def findByCelebrity(celebrityId: Long, filters: FilterOneTable[Product] *): Query[Product] = {
    from(schema.products)(product =>
      where(
        product.celebrityId === celebrityId and
          FilterOneTable.reduceFilters(filters, product)
      )
      select(product)
    )
  }

  def findByCelebrityAndUrlSlug(celebrityId: Long, slug: String): Option[Product] = {
    if (slug.isEmpty) return None

    from(schema.products)(product =>
      where(product.celebrityId === celebrityId and product.urlSlug === slug)
        select (product)
    ).headOption
  }

  def products(inventoryBatch: InventoryBatch): Query[Product] with ManyToMany[Product, InventoryBatchProduct] = {
    schema.inventoryBatchProducts.left(inventoryBatch)
  }

  //
  // SavesWithLongKey[Product] methods
  //
  def table = schema.products

  override def defineUpdate(theOld: Product, theNew: Product) = {
    updateIs(
      theOld.celebrityId := theNew.celebrityId,
      theOld.priceInCurrency := theNew.priceInCurrency,
      theOld.name := theNew.name,
      theOld.urlSlug := theNew.urlSlug,
      theOld.photoKey := theNew.photoKey,
      theOld.description := theNew.description,
      theOld._defaultFrameName := theNew._defaultFrameName,
      theOld._iconKey := theNew._iconKey,
      theOld.storyTitle := theNew.storyTitle,
      theOld.storyText := theNew.storyText,
      theOld.signingScaleW := theNew.signingScaleW,
      theOld.signingScaleH := theNew.signingScaleH,
      theOld.signingOriginX := theNew.signingOriginX,
      theOld.signingOriginY := theNew.signingOriginY,
      theOld.signingAreaW := theNew.signingAreaW,
      theOld.signingAreaH := theNew.signingAreaH,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated,
      theOld._publishedStatus := theNew._publishedStatus
    )
  }

  //
  // SavesCreatedUpdated[Long,Product] methods
  //
  override def withCreatedUpdated(toUpdate: Product, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}

case class InventoryQuantity(quantityRemaining: Int, ibStartDate: Date, ibEndDate: Date)

class ProductQueryFilters {
  import org.squeryl.PrimitiveTypeMode._

  def byUrlSlug(slug: String): FilterOneTable[Product] = {
    new FilterOneTable[Product] {
      override def test(product: Product) = {
        product.urlSlug === slug
      }
    }
  }
}
