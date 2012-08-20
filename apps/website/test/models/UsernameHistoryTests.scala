package models

import enums.OrderReviewStatus
import utils._
import services.{Utils, Time, AppConfig}
import exception.InsufficientInventoryException
import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers

class UsernameHistoryTests extends EgraphsUnitTest
with ClearsCacheAndBlobsAndValidationBefore
with SavingEntityIdStringTests[UsernameHistory]
with CreatedUpdatedEntityTests[String, UsernameHistory]
with DBTransactionPerTest
{
  val usernameHistoryStore = AppConfig.instance[UsernameHistoryStore]
  val customerStore = AppConfig.instance[CustomerStore]

  //
  // SavingEntityTests[Account] methods
  //
  override def newEntity = {
    models.UsernameHistory(id = newIdValue)
  }

  override def saveEntity(toSave: UsernameHistory) = {
    val customer = Customer(name = TestData.generateFullname(), username = TestData.generateUsername()).save()
    toSave.copy(customerId = customer.id).save()
  }

  override def restoreEntity(id: String) = {
    usernameHistoryStore.findById(id)
  }

  override def transformEntity(toTransform: UsernameHistory) = {
    toTransform.copy(
      isPermanent = !toTransform.isPermanent
    )
  }

  //
  // Test cases
  //
  "UsernameHistory" should "require certain fields" in {
    val exception = intercept[RuntimeException] {UsernameHistory().save()}
    exception.getLocalizedMessage should include("ERROR: insert or update on table \"usernamehistory\" violates foreign key constraint")
  }

  "A UsernameHistory" should "be able to have multiple customers with the same username" in {
    val customer = TestData.newSavedCustomer()
    val usernameHistory = usernameHistoryStore.findByCustomer(customer)
    usernameHistory.length should be (1)
    val defaultUsername = usernameHistory.head

    // add user chosen username for the customer.
    val chosenUsername = UsernameHistory( id = TestData.generateUsername(), customerId = customer.id, isPermanent = true).save()
    val usernameHistoryAfter = usernameHistoryStore.findByCustomer(customer)
    usernameHistoryAfter.length should be (2)
  }
}