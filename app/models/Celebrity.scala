package models

import org.squeryl.PrimitiveTypeMode._
import java.sql.Timestamp
import db.{KeyedCaseClass, Schema, Saves}
import libs.{Serialization, Time}
import libs.Blobs.AccessPolicy

/**
 * Persistent entity representing the Celebrities who provide products on
 * our service.
 */
case class Celebrity(
  id: Long = 0,
  apiKey: Option[String] = None,
  description: Option[String] = None,
  firstName: Option[String]   = None,
  lastName: Option[String]    = None,
  popularName: Option[String] = None,
  profilePhotoUpdated: Option[Timestamp] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  //
  // Public methods
  //
  /** Persists by conveniently delegating to companion object's save method. */
  def save(): Celebrity = {
    Celebrity.save(this)
  }

  /**
   * Renders the Celebrity as a Map, which will itself be rendered into whichever data format
   * by the API (e.g. JSON)
   */
  def renderedForApi: Map[String, Any] = {
    val optionalFields = List(
      ("firstName" -> firstName),
      ("lastName" -> lastName),
      ("popularName" -> popularName)
    )

    Map("id" -> id) ++
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
    val celebrityToSave = this.copy(profilePhotoUpdated = Some(Time.now))
    val assetName = celebrityToSave.profilePhotoAssetNameOption.get
    val image = ImageAsset(imageData, keyBase, assetName, ImageAsset.Png)

    // Upload the image then save the entity, confident that the resulting entity
    // will have a valid master image.
    image.save(AccessPolicy.Public)

    (celebrityToSave.save(), image)
  }

  /**
   * Returns the profile photo image asset for this celebrity if one was ever stored using
   * `saveWithProfilePhoto`.
   */
  def profilePhoto: Option[ImageAsset] = {
    for (profilePhotoAssetName <- profilePhotoAssetNameOption) yield {
      ImageAsset(keyBase, profilePhotoAssetName, ImageAsset.Png)
    }
  }

  /** Creates a new Product associated with the celebrity. The product is not yet persisted. */
  def newProduct: Product = {
    Product(celebrityId=id)
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
      "profile_" + Time.toBlobstoreFormat(photoUpdatedTimestamp).replace(" ", "")
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

}

object Celebrity extends Saves[Celebrity] with SavesCreatedUpdated[Celebrity] {
  //
  // Public Methods
  //
  def findByName(name: String): Option[Celebrity] = {
    from(Schema.celebrities)(celebrity =>
      where(celebrity.popularName === Some(name))
      select(celebrity)
    ).headOption
  }

  //
  // Saves[Celebrity] methods
  //
  override val table = Schema.celebrities

  override def defineUpdate(theOld: Celebrity, theNew: Celebrity) = {
    updateIs(
      theOld.apiKey := theNew.apiKey,
      theOld.description := theNew.description,
      theOld.firstName := theNew.firstName,
      theOld.lastName := theNew.lastName,
      theOld.popularName := theNew.popularName,
      theOld.profilePhotoUpdated := theNew.profilePhotoUpdated,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Celebrity] methods
  //
  override def withCreatedUpdated(toUpdate: Celebrity, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}
