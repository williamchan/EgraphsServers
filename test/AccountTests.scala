import models.Account
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.data.validation.Validation
import play.test.UnitFlatSpec
import scala.collection.JavaConversions._


class AccountTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with CreatedUpdatedEntityTests[Account]
{
  //
  // CreatedUpdatedEntityTests[Account] methods
  //
  override def getCreatedUpdatedEntity = {
    Account()
  }
  
  override def saveCreatedUpdated(toSave: Account) = {
    Account.save(toSave)
  }

  //
  // Fixtures
  //
  override def afterEach() {
    Validation.clear()
    db.Schema.scrub
  }

  //
  // Test methods
  //
  "An Account" should "start unprotected" in {
    Account().password should be (None)
  }

  it should "become protected once a password is set" in {
    accountWithPassword("herp").password should not be (None)
  }

  it should "have different hashes and salts when the same password is set twice" in {
    val credential = accountWithPassword("herp")
    val firstPassword = credential.password.get

    val password = credential.withPassword("herp").right.get.password.get

    firstPassword.is("herp") should be (true)
    password.is("herp") should be (true)
    password.hash should not be (firstPassword.hash)
    password.salt should not be (firstPassword.salt)
  }

  it should "store and retrieve correctly" in {
    // Set up
    val stored = accountWithPassword("herp")
    val storedPassword = stored.password.get

    // Run test
    val saved = Account.save(stored)
    val maybeRecalled = Account.byId(saved.id)

    // Check expectations
    maybeRecalled should not be (None)

    val recalled = maybeRecalled.get
    val recalledPassword = recalled.password.get
    recalled.id should be (stored.id)
    recalledPassword should be (storedPassword)
    recalledPassword.is("herp") should be (true)
  }

  it should "fail validation for password lengths shorter than 4 characters" in {
    for (password <- List("", "123")) {
      val errorOrCredential = Account().withPassword(password)
      errorOrCredential.isLeft should be (true)

      Validation.errors should have length (1)
      Validation.errors.head.getKey should be ("password")
      Validation.clear()
    }
  }

  it should "pass validation for password lengths 4 characters and longer" in {
    Account().withPassword("herp").isRight should be (true)

    Validation.errors should have length (0)
  }

  def accountWithPassword(password: String): Account = {
    Account.save(Account().withPassword(password).right.get)
  }
}