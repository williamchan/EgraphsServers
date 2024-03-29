package models

import enums.OrderReviewStatus
import utils._
import services.{Utils, Time, AppConfig}
import exception.InsufficientInventoryException
import org.scalatest.matchers.ShouldMatchers
import scala.None

class UsernameTests extends EgraphsUnitTest
  with ClearsCacheBefore
  with SavingEntityIdStringTests[Username]
  with CreatedUpdatedEntityTests[String, Username]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  private def usernameHistoryStore = AppConfig.instance[UsernameHistoryStore]
  private def customerStore = AppConfig.instance[CustomerStore]

  //
  // SavingEntityTests[Account] methods
  //
  override def newEntity = {
    models.Username(id = newIdValue)
  }

  override def saveEntity(toSave: Username) = {
    val customer = Customer(name = TestData.generateFullname(), username = TestData.generateUsername()).save()
    toSave.copy(customerId = customer.id).save()
  }

  override def restoreEntity(id: String) = {
    usernameHistoryStore.findById(id)
  }

  override def transformEntity(toTransform: Username) = {
    toTransform.copy(
      isPermanent = !toTransform.isPermanent
    )
  }

  //
  // Test cases
  //
  "Username" should "require certain fields" in new EgraphsTestApplication {
    val exception = intercept[RuntimeException] {Username().save()}
    exception.getLocalizedMessage should include("ERROR: insert or update on table \"usernames\" violates foreign key constraint")
  }

  "Multiple Usernames" should "be able to belong to a customer" in new EgraphsTestApplication {
    val customer = TestData.newSavedCustomer()
    val usernameHistory = usernameHistoryStore.findAllByCustomer(customer)
    usernameHistory.length should be (1)
    val defaultUsername = usernameHistory.head

    // add user chosen username for the customer.
    val chosenUsername = Username( id = TestData.generateUsername(), customerId = customer.id, isPermanent = true).save()
    val usernameHistoryAfter = usernameHistoryStore.findAllByCustomer(customer)
    usernameHistoryAfter.length should be (2)
  }

  "There" should "be only one permanent Username for a Customer" in new EgraphsTestApplication {
    val customer = TestData.newSavedCustomer()
    val usernameHistory = usernameHistoryStore.findAllByCustomer(customer)
    usernameHistory.length should be (1)
    usernameHistory.head.copy(isPermanent = true).save() //make the username permanent

    // try to make another username permanent should fail
    val exception = intercept[Exception] {Username( id = TestData.generateUsername(), customerId = customer.id, isPermanent = true).save()}
    exception.getLocalizedMessage should include("There can only be one permanent username")
  }

  "Username" should "not be the current username for a customer if it is removed" in new EgraphsTestApplication {
    val customer = TestData.newSavedCustomer()
    val usernameHistory = usernameHistoryStore.findAllByCustomer(customer)
    usernameHistory.length should be (1)
    usernameHistory.head.copy(isRemoved = true).save() //make the username removed

    val currentUsername = usernameHistoryStore.findCurrentByCustomer(customer)
    currentUsername should be (None)
  }
}