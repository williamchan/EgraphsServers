package models

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterEach
import play.test.UnitFlatSpec
import utils.{DBTransactionPerTest, ClearsDatabaseAndValidationAfter, CreatedUpdatedEntityTests, SavingEntityTests}
import services.Time
import services.AppConfig

class ProductTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Product]
  with CreatedUpdatedEntityTests[Product]
  with ClearsDatabaseAndValidationAfter
  with DBTransactionPerTest
{
  val store = AppConfig.instance[ProductStore]

  //
  // SavingEntityTests[Product] methods
  //
  override def newEntity = {
    Celebrity().save().newProduct
  }

  override def saveEntity(toSave: Product) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: Product) = {
    toTransform.copy(
      priceInCurrency = 1000,
      name = "NBA Championships 2010",
      photoKey = Some(Time.toBlobstoreFormat(Time.now)),
      description = "Shaq goes for the final dunk in the championship"
    )
  }

  //
  // Test cases
  //
  "A product" should "serialize the correct Map for the API" in {
    val product = Celebrity().save().newProduct.copy(name="Herp Derp").save()

    val rendered = product.renderedForApi
    rendered("id") should be (product.id)
    rendered("photoUrl") should be (product.photo.url)
    rendered("urlSlug") should be ("Herp-Derp")
    rendered.contains("created") should be (true)
    rendered.contains("updated") should be (true)
  }

}