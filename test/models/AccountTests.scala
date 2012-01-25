package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.data.validation.Validation
import play.test.UnitFlatSpec
import scala.collection.JavaConversions._
import utils.{DBTransactionPerTest, SavingEntityTests, CreatedUpdatedEntityTests, ClearsDatabaseAndValidationAfter}
import services.AppConfig

class AccountTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterEach
with SavingEntityTests[Account]
with CreatedUpdatedEntityTests[Account]
with ClearsDatabaseAndValidationAfter
with DBTransactionPerTest {
  import AppConfig.instance

  val accountStore = instance[AccountStore]
  val celebrityStore = instance[CelebrityStore]
  val customerStore = instance[CustomerStore]
  val administratorStore = instance[AdministratorStore]

  //
  // SavingEntityTests[Account] methods
  //
  def newEntity = {
    Account()
  }

  def saveEntity(toSave: Account) = {
    accountStore.save(toSave)
  }

  def restoreEntity(accountId: Long) = {
    accountStore.findById(accountId)
  }

  override def transformEntity(toTransform: Account) = {
    celebrityStore.save(Celebrity())
    customerStore.save(Customer())
    administratorStore.save(Administrator())
    toTransform.copy(
      email = "derp",
      passwordHash = Some("derp"),
      passwordSalt = Some("derp"),
      celebrityId = Some(1L),
      customerId = Some(1L),
      administratorId = Some(1L)
    )
  }

  //
  // Test methods
  //
  "An Account" should "start unprotected" in {
    Account().password should be(None)
  }

  it should "become protected once a password is set" in {
    accountWithPassword("derp").password should not be (None)
  }

  it should "have different hashes and salts when the same password is set twice" in {
    val credential = accountWithPassword("derp")
    val firstPassword = credential.password.get

    val password = credential.withPassword("derp").right.get.password.get

    firstPassword.is("derp") should be(true)
    password.is("derp") should be(true)
    password.hash should not be (firstPassword.hash)
    password.salt should not be (firstPassword.salt)
  }

  it should "store and retrieve correctly" in {
    // Set up
    val stored = accountWithPassword("derp")
    val storedPassword = stored.password.get

    // Run test
    val saved = accountStore.save(stored)
    val maybeRecalled = accountStore.findById(saved.id)

    // Check expectations
    maybeRecalled should not be (None)

    val recalled = maybeRecalled.get
    val recalledPassword = recalled.password.get
    recalled.id should be(stored.id)
    recalledPassword should be(storedPassword)
    recalledPassword.is("derp") should be(true)
  }

  it should "fail validation for password lengths shorter than 4 characters" in {
    for (password <- List("", "123")) {
      val errorOrCredential = Account().withPassword(password)
      errorOrCredential.isLeft should be(true)

      Validation.errors should have length (1)
      Validation.errors.head.getKey should be("password")
      Validation.clear()
    }
  }

  it should "pass validation for password lengths 4 characters and longer" in {
    Account().withPassword("derp").isRight should be(true)

    Validation.errors should have length (0)
  }

  it should "should persist fine with no celebrity/customer/admin IDs" in {
    accountStore.save(Account()) // Doesn't throw any errors
  }

  it should "fail to persist with non-null, non-existent celebrity ID" in {
    val thrown = evaluating {
      accountStore.save(Account(celebrityId = Some(1L)))
    } should produce[RuntimeException]
    thrown.getMessage.toUpperCase.contains("CELEBRITYID") should be(true)
  }

  it should "fail to persist with non-null, non-existent customer ID" in {
    val thrown = evaluating {
      accountStore.save(Account(customerId = Some(1L)))
    } should produce[RuntimeException]
    thrown.getMessage.toUpperCase.contains("CUSTOMERID") should be(true)
  }

  it should "fail to persist with non-null, non-existent administrator ID" in {
    val thrown = evaluating {
      accountStore.save(Account(administratorId = Some(1L)))
    } should produce[RuntimeException]
    thrown.getMessage.toUpperCase.contains("ADMINISTRATORID") should be(true)
  }

  it should "be recoverable by email" in {
    val stored = Account(email = "derp@derp.com").save()
    accountStore.findByEmail(stored.email) should be(Some(stored))
  }

  it should "authenticate the correct email and password in" in {
    val stored = savedAccountWithEmailAndPassword("derp@derp.com", "supersecret")
    accountStore.authenticate("derp@derp.com", "supersecret") should be(Right(stored))
  }

  it should "fail to authenticate with an AccountCredentialsError if the password is wrong" in {
    savedAccountWithEmailAndPassword("derp@derp.com", "supersecret")
    accountStore.authenticate("derp@derp.com", "superWRONG") match {
      case Left(correct: AccountCredentialsError) => // phew
      case anythingElse => fail(anythingElse + " should have been a credentials error")
    }
  }

  it should "fail to authenticate with an AccountPasswordNotSetError if the account wasn't protected" in {
    Account(email = "derp@derp.com").save()
    accountStore.authenticate("derp@derp.com", "supersecret") match {
      case Left(correct: AccountPasswordNotSetError) => // phew
      case anythingElse => fail(anythingElse + " should have been an AccountPasswordNotSetError")
    }
  }

  it should "fail to authenticate with an AccountNotFoundError if an account with the given email didnt exist" in {
    accountStore.authenticate("derp@derp.com", "supersecret") match {
      case Left(correct: AccountNotFoundError) => // phew
      case anythingElse => fail(anythingElse + " should have been an AccountNotFoundError")
    }
  }

  it should "save email in lowercase" in {
    val stored = Account(email = "DERP@DERP.COM").save()
    accountStore.findByEmail("derp@derp.com") should be(Some(stored))
  }

  it should "save email trimmed" in {
    val stored = Account(email = "                    derp@derp.com                    ").save()
    accountStore.findByEmail("derp@derp.com") should be(Some(stored))
  }

  it should "find by email case-insensitively" in {
    val stored = Account(email = "derp@derp.com").save()
    accountStore.findByEmail("DERP@DERP.COM") should be(Some(stored))
  }

  def accountWithPassword(password: String): Account = {
    accountStore.save(Account().withPassword(password).right.get)
  }

  def savedAccountWithEmailAndPassword(email: String, password: String): Account = {
    Account(email = "derp@derp.com").withPassword("supersecret").right.get.save()
  }
}