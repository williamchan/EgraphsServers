package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils.{DBTransactionPerTest, SavingEntityTests, CreatedUpdatedEntityTests, ClearsDatabaseAndValidationBefore}
import services.AppConfig
import services.Time

class AdministratorTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Administrator]
  with CreatedUpdatedEntityTests[Administrator]
  with ClearsDatabaseAndValidationBefore
  with DBTransactionPerTest
{
  val adminStore = AppConfig.instance[AdministratorStore]

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
    toTransform.copy(role = Some("big boss man"))
  }


  //
  // Test cases
  //

  "authenticate" should "return Administrator with correct credentials, else return None" in {
    val pw = "derp"
    val acct = Account(email = "customer-" + Time.toBlobstoreFormat(Time.now) + "@egraphs.com").withPassword(pw).right.get.save()
    adminStore.authenticate(email = acct.email, passwordAttempt = pw) should be(None)

    val administrator = Administrator().save()
    acct.copy(administratorId = Some(administrator.id)).save()
    adminStore.authenticate(email = acct.email, passwordAttempt = pw).get should be(administrator)
    adminStore.authenticate(email = acct.email, passwordAttempt = "wrongpassword") should be(None)
    adminStore.authenticate(email = "wrongemail@egraphs.com", passwordAttempt = pw) should be(None)
  }

  "findByEmail" should "find Administrator by email" in {
    val acct = Account(email = "customer-" + Time.toBlobstoreFormat(Time.now) + "@egraphs.com").save()
    adminStore.findByEmail(acct.email) should be(None)

    val administrator = Administrator().save()
    acct.copy(administratorId = Some(administrator.id)).save()
    adminStore.findByEmail(acct.email).get should be(administrator)
  }

}