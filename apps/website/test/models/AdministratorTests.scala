package models

import enums.AdminRole
import services.AppConfig
import services.Time
import utils._

class AdministratorTests extends EgraphsUnitTest
  with ClearsCacheBefore
  with SavingEntityIdLongTests[Administrator]
  with CreatedUpdatedEntityTests[Long, Administrator]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  def adminStore = AppConfig.instance[AdministratorStore]

  //
  // SavingEntityTests[Account] methods
  //
  override def newEntity = {
    Administrator()
  }

  override def saveEntity(toSave: Administrator) = {
    adminStore.save(toSave)
  }

  override def restoreEntity(id: Long) = {
    adminStore.findById(id)
  }

  override def transformEntity(toTransform: Administrator) = {
    // No Administrator-specific data here to transform yet!
    toTransform.withRole(AdminRole.AdminDisabled)
  }


  //
  // Test cases
  //

  "isAdmin" should "return true if id exists for Administrator, false otherwise" in new EgraphsTestApplication {
    val administrator = Administrator().save()
    adminStore.isAdmin(Some(administrator.id)) should be(true)
    adminStore.isAdmin(Some(Long.MaxValue)) should be(false)
    adminStore.isAdmin(None) should be(false)
  }

  "authenticate" should "return Administrator with correct credentials, else return None" in new EgraphsTestApplication {
    val pw = TestData.defaultPassword
    val acct = Account(email = "customer-" + Time.toBlobstoreFormat(Time.now) + "@egraphs.com").withPassword(pw).right.get.save()
    adminStore.authenticate(email = acct.email, passwordAttempt = pw) should be(None)

    val administrator = Administrator().save()
    acct.copy(administratorId = Some(administrator.id)).save()
    adminStore.authenticate(email = acct.email, passwordAttempt = pw).get should be(administrator)
    adminStore.authenticate(email = acct.email, passwordAttempt = "wrongpassword") should be(None)
    adminStore.authenticate(email = "wrongemail@egraphs.com", passwordAttempt = pw) should be(None)
  }

  "findByEmail" should "find Administrator by email" in new EgraphsTestApplication {
    val acct = Account(email = "customer-" + Time.toBlobstoreFormat(Time.now) + "@egraphs.com").save()
    adminStore.findByEmail(acct.email) should be(None)

    val administrator = Administrator().save()
    acct.copy(administratorId = Some(administrator.id)).save()
    adminStore.findByEmail(acct.email).get should be(administrator)
  }

}