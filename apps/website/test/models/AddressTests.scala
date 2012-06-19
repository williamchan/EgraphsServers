package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.AppConfig

class AddressTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Address]
  with CreatedUpdatedEntityTests[Address]
  with ClearsDatabaseAndValidationBefore
  with DBTransactionPerTest {

  private val addressStore = AppConfig.instance[AddressStore]

  //
  // SavingEntityTests[Address] methods
  //
  override def newEntity = {
    val account = TestData.newSavedAccount()
    Address(accountId = account.id,
      addressLine1 = "Celebrity Marketplace",
      addressLine2 = "202 29th Ave",
      city = "Seattle",
      state = "WA",
      postalCode = "98122"
    )
  }

  override def saveEntity(toSave: Address) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    addressStore.findById(id)
  }

  override def transformEntity(toTransform: Address) = {
    toTransform.copy(
      postalCode = "01810")
  }

  //
  // Test cases
  //
  "account" should "return the associated Account" in {
    val account = TestData.newSavedAccount()
    val address = Address(accountId = account.id).save()
    address.account should be(account)
  }

  "findByAccount" should "return all associated Addresses" in {
    val account = TestData.newSavedAccount()
    val address1 = Address(accountId = account.id).save()
    val address2 = Address(accountId = account.id).save()
    addressStore.findByAccount(account.id).toSet should be(Set(address1, address2))
  }
}
