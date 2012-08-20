package models

import play.data.validation.Validation
import scala.collection.JavaConversions._
import services.AppConfig
import AppConfig.instance
import AccountAuthenticationError._
import utils._

class AccountTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with DBTransactionPerTest
  with AccountTestHelpers
{

  val accountStore = instance[AccountStore]

  //
  // Test methods
  //
  "Account" should "require certain fields" in {
    val exception = intercept[IllegalArgumentException] {Account().save()}
    exception.getLocalizedMessage should include("Account: email must be specified")
  }

  "An Account" should "start without a password" in {
    Account().password should be(None)
  }

  "An Account" should "start without email verification" in {
    Account().emailVerified should be(false)
  }

  "emailVerify" should "set the account to verified" in {
    Account().emailVerify().emailVerified should be(true)
  }

  "withPassword" should "set the password on an Account" in {
    accountWithPassword(TestData.defaultPassword).password should not be (None)
  }

  "An Account" should "have different hashes and salts when the same password is set twice" in {
    val credential = accountWithPassword(TestData.defaultPassword)
    val firstPassword = credential.password.get

    val password = credential.withPassword(TestData.defaultPassword).right.get.password.get

    firstPassword.is(TestData.defaultPassword) should be(true)
    password.is(TestData.defaultPassword) should be(true)
    password.hash should not be (firstPassword.hash)
    password.salt should not be (firstPassword.salt)
  }

  it should "fail validation for password lengths shorter than 8 characters" in {
    for (password <- List("", "egraphs")) {
      val errorOrCredential = Account().withPassword(password)
      errorOrCredential.isLeft should be(true)

      Validation.errors should have length (1)
      Validation.errors.head.getKey should be("password")
      Validation.clear()
    }
  }

  it should "pass validation for password lengths 8 characters and longer" in {
    Account().withPassword(TestData.defaultPassword).isRight should be(true)

    Validation.errors should have length (0)
  }

  it should "fail to authenticate with an AccountCredentialsError if the password is wrong" in {
    savedAccountWithEmailAndPassword("derp@derp.com", "supersecret")
    accountStore.authenticate("derp@derp.com", "superWRONG") match {
      case Left(correct: AccountCredentialsError) => // phew
      case anythingElse => fail(anythingElse + " should have been a credentials error")
    }
  }

  it should "fail to authenticate with an AccountPasswordNotSetError if the account lacks a password" in {
    val account = Account(email = "derp@derp.com").save()
    account.password should be(None)
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

  "withResetPasswordKey" should "set resetPasswordKey to newly generated key" in {
    var account = TestData.newSavedAccount()
    account.resetPasswordKey should be(None)
    account = account.withResetPasswordKey.save()
    account.resetPasswordKey.get should not be(None)
    account.resetPasswordKey.get.lastIndexOf('.') should not be (-1)
  }

  "verifyResetPasswordKey" should "return false if resetPasswordKey is None" in {
    var account = TestData.newSavedAccount()
    account.verifyResetPasswordKey("whatever") should be(false)
    account = account.withResetPasswordKey.save()
    account.verifyResetPasswordKey("whatever.") should be(false)
    account.verifyResetPasswordKey(account.resetPasswordKey.get) should be(true)
  }

  "verifyResetPasswordKey" should "return false if resetPasswordKey is expired" in {
    var account = TestData.newSavedAccount().withResetPasswordKey.save()
    val oldKey = account.resetPasswordKey.get
    val newKey = oldKey.substring(0, oldKey.lastIndexOf(".") + 1) + System.currentTimeMillis
    account = account.copy(resetPasswordKey = Some(newKey)).save()
    account.verifyResetPasswordKey(account.resetPasswordKey.get) should be(false)
  }

  "withPassword" should "set resetPasswordKey to None" in {
    var account = TestData.newSavedAccount().withResetPasswordKey.save()
    account = account.withPassword("newpassword").right.get.save()
    account.resetPasswordKey should be(None)
  }

  "An account" should "save email in lowercase" in {
    val stored = Account(email = "DERP@DERP.COM").save()
    accountStore.findByEmail("derp@derp.com") should be(Some(stored))
  }

  it should "save email trimmed" in {
    val stored = Account(email = "                    derp@derp.com                    ").save()
    accountStore.findByEmail("derp@derp.com") should be(Some(stored))
  }

  "createCustomer" should "create Customer with username based on email" in {
    val account = TestData.newSavedAccount()
    val customer = account.createCustomer("Test User").save()
    account.email should include(customer.username)
  }

  "createCustomer" should "throw exception if called on account that already has a customer" in {
    val customer = TestData.newSavedCustomer()
    intercept[IllegalArgumentException] {customer.account.createCustomer("Another User")}
  }

  "findByEmail" should "find by email case-insensitively" in {
    val stored = Account(email = "derp@derp.com").save()
    accountStore.findByEmail("DERP@DERP.COM") should be(Some(stored))
  }
}

class AccountStoreTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityIdLongTests[Account]
  with CreatedUpdatedEntityTests[Long, Account]
  with DBTransactionPerTest
  with AccountTestHelpers
{
  import AppConfig.instance

  def celebrityStore = instance[CelebrityStore]
  def customerStore = instance[CustomerStore]
  def administratorStore = instance[AdministratorStore]

  def accountStore = instance[AccountStore]

  //
  // SavingEntityTests[Account] methods
  //
  override def newEntity = {
    Account()
  }

  override def saveEntity(toSave: Account) = {
    accountStore.save(toSave)
  }

  override def restoreEntity(accountId: Long) = {
    accountStore.findById(accountId)
  }

  override def transformEntity(toTransform: Account) = {
    toTransform.copy(
      email = "derp",
      passwordHash = Some(TestData.defaultPassword),
      passwordSalt = Some(TestData.defaultPassword),
      celebrityId = Some(Celebrity().save().id),
      customerId = Some(Customer(name = "name", username = "username").save().id),
      administratorId = Some(Administrator().save().id)
    )
  }

  it should "store and retrieve correctly" in {
    // Set up
    val stored = accountWithPassword(TestData.defaultPassword)
    val storedPassword = stored.password.get

    // Run test
    val underTest = accountStore
    val saved = underTest.save(stored)
    val maybeRecalled = underTest.findById(saved.id)

    // Check expectations
    maybeRecalled should not be (None)

    val recalled = maybeRecalled.get
    val recalledPassword = recalled.password.get
    recalled.id should be(stored.id)
    recalledPassword should be(storedPassword)
    recalledPassword.is(TestData.defaultPassword) should be(true)
  }
  
  it should "find by customer ID correctly" in {
    val customer = TestData.newSavedCustomer()
    val account = customer.account
    accountStore.findByCustomerId(customer.id) should be (Some(account))
    accountStore.findByCustomerId(customer.id + 1) should be (None)
  }

  it should "should persist fine with no celebrity/customer/admin IDs" in {
    accountStore.save(Account(email = "email@egraphs.com")) // Doesn't throw any errors
  }

  it should "fail to persist with non-null, non-existent celebrity ID" in {
    val thrown = evaluating {
                              accountStore.save(Account(celebrityId = Some(1L)))
                            } should produce[RuntimeException]
    thrown.getMessage.toUpperCase should include("CELEBRITYID")
  }

  it should "fail to persist with non-null, non-existent customer ID" in {
    val thrown = evaluating {
                              accountStore.save(Account(customerId = Some(1L)))
                            } should produce[RuntimeException]
    thrown.getMessage.toUpperCase should include("CUSTOMERID")
  }

  it should "fail to persist with non-null, non-existent administrator ID" in {
    val thrown = evaluating {
                              accountStore.save(Account(administratorId = Some(1L)))
                            } should produce[RuntimeException]
    thrown.getMessage.toUpperCase should include("ADMINISTRATORID")
  }

  it should "be recoverable by email" in {
    accountStore.findByEmail(null) should be(None)
    accountStore.findByEmail("") should be(None)
    val stored = Account(email = "derp@derp.com").save()
    accountStore.findByEmail(stored.email) should be(Some(stored))
  }

  it should "authenticate the correct email and password in" in {
    val stored = savedAccountWithEmailAndPassword("derp@derp.com", "supersecret")
    accountStore.authenticate("derp@derp.com", "supersecret") should be(Right(stored))
  }

}

trait AccountTestHelpers {
  def accountStore: AccountStore

  def accountWithPassword(password: String): Account = {
    accountStore.save(Account(email = "email@egraphs.com").withPassword(password).right.get)
  }

  def savedAccountWithEmailAndPassword(email: String, password: String): Account = {
    Account(email = "derp@derp.com").withPassword("supersecret").right.get.save()
  }
}