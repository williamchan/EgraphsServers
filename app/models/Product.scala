package models

import java.sql.Timestamp
import org.squeryl.Query
import services.db.{FilterOneTable, Schema, Saves, KeyedCaseClass}
import play.templates.JavaExtensions
import org.joda.money.Money
import services.Finance.TypeConversions._
import models.Product.ProductWithPhoto
import services.blobs.Blobs.Conversions._
import java.awt.image.BufferedImage
import play.Play
import services.blobs.AccessPolicy
import com.google.inject.{Provider, Inject}
import services._

case class ProductServices @Inject() (
  store: ProductStore,
  celebStore: CelebrityStore,
  templateEngine: TemplateEngine,
  imageAssetServices: Provider[ImageAssetServices]
)

/**
 * An item on sale by a Celebrity. In the case of the base Egraph, it represents a signature service
 * against a particular photograph of the celebrity.
 */
case class Product(
  id: Long = 0L,
  celebrityId: Long = 0L,
  priceInCurrency: BigDecimal = 0,
  name: String = "",
  description: String = "",
  _defaultFrameName: String = PortraitEgraphFrame.name,
  storyTitle: String = "The Story",
  storyText: String = "(No story has been written for this product)",
  photoKey: Option[String] = None,
  _iconKey: Option[String] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: ProductServices = AppConfig.instance[ProductServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated {

  //
  // Additional DB columns
  //
  /** The slug used to access this product from the main site */
  val urlSlug = Product.slugify(name)

  //
  // Public members
  //
  def save(): Product = {
    services.store.save(this)
  }

  def saveWithImageAssets(image: Option[BufferedImage], icon: Option[BufferedImage]): Product = {
    import ImageUtil.Conversions._

    // Prepare the product photo, cropped to the suggested frame
    val product = if (image.isDefined) {
      val frame = EgraphFrame.suggestedFrame(Dimensions(image.get.getWidth, image.get.getHeight))
      val imageCroppedToFrame = frame.cropImageForFrame(image.get)
      val imageByteArray = imageCroppedToFrame.asByteArray(ImageAsset.Jpeg)
      // Product have previously been saved so that it has an ID for blobstore to key on
      val savedWithFrame = withFrame(frame).save()
      savedWithFrame.withPhoto(imageByteArray).save().product
    } else {
      this
    }

    // Prepare the product plaque icon, cropped to a square
    if (icon.isDefined) {
      val iconCroppedToSquare = ImageUtil.cropToSquare(icon.get)
      val iconBytes = iconCroppedToSquare.asByteArray(ImageAsset.Jpeg)
      product.withIcon(iconBytes).save()
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

  def withIcon(iconImageData:Array[Byte]): ProductWithPhoto = {
    val newIconKey = "icon_" + Time.toBlobstoreFormat(Time.now)

    ProductWithPhoto(
      product=this.copy(_iconKey=Some(newIconKey)),
      photo=ImageAsset(iconImageData, keyBase, newIconKey, ImageAsset.Png, services.imageAssetServices.get)
    )
  }

  def withPhoto(imageData:Array[Byte]): ProductWithPhoto = {
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
      "photoUrl" -> photo.url,
      "urlSlug" -> urlSlug
    )
  }

  /** Retrieves the celebrity from the database */
  def celebrity: Celebrity = {
    services.celebStore.get(celebrityId)
  }
  
  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = {
    Product.unapply(this)
  }

  //
  // Private members
  //
  private def keyBase = {
    require(id > 0, "Can not determine blobstore key when no id exists yet for this entity in the relational database")

    "product/" + id
  }

  lazy val defaultPhoto = ImageAsset(
      Play.getFile("test/files/longoria/product-2.jpg"),
      keyBase="defaults/product",
      name="photo",
      imageType=ImageAsset.Jpeg,
      services=services.imageAssetServices.get
  )

  lazy val defaultIcon = ImageAsset(
    Play.getFile("public/images/egraph_default_plaque_icon.png"),
    keyBase="defaults/product",
    name="photo_icon",
    imageType=ImageAsset.Png,
    services=services.imageAssetServices.get
  )
}

object Product {

  val defaultPrice = 50

  def slugify(productName: String): String = {
    JavaExtensions.slugify(productName, false) // Slugify without lower-casing
  }

  case class ProductWithPhoto(product: Product, photo: ImageAsset) {
    def save(): ProductWithPhoto = {
      val savedPhoto = photo.save(AccessPolicy.Public)
      val savedProduct = product.save()

      ProductWithPhoto(savedProduct, savedPhoto)
    }
  }
}

class ProductStore @Inject() (schema: Schema) extends Saves[Product] with SavesCreatedUpdated[Product] {
  import org.squeryl.PrimitiveTypeMode._

  //
  // Public members
  //
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

  //
  // Saves[Product] methods
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
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Product] methods
  //
  override def withCreatedUpdated(toUpdate: Product, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}

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