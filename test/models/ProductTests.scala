package models

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterEach
import play.test.UnitFlatSpec
import utils.{ClearsDatabaseAndValidationAfter, CreatedUpdatedEntityTests, SavingEntityTests}

class ProductTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Product]
  with CreatedUpdatedEntityTests[Product]
  with ClearsDatabaseAndValidationAfter
{
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
    Product.findById(id)
  }

  override def transformEntity(toTransform: Product) = {
    toTransform.copy(
      priceInCents = 1000,
      description = Some("NBA Championships 2010")
    )
  }

  //
  // Test cases
  //

}