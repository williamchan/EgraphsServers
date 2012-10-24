package models

import scala.collection.JavaConversions._
import services.AppConfig
import AccountAuthenticationError._
import utils._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AccountTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with DBTransactionPerTest
  with AccountTestHelpers
{

  def accountStore = AppConfig.instance[AccountStore]

  //
  // Test methods
  //
  "An Account" should "require certain fields" in new EgraphsTestApplication {
    val exception = intercept[IllegalArgumentException] {Account().save()}
    exception.getLocalizedMessage should include("Account: email must be specified")
  }

  it should "start without a password" in new EgraphsTestApplication {
    Account().password should be(None)
  }

  it should "start without email verification" in new EgraphsTestApplication {
    Account().emailVerified should be(false)
  }

  "emailVerify" should "set the account to verified" in new EgraphsTestApplication {
    Account().emailVerify().emailVerified should be(true)
  }

  "withPassword" should "set the password on an Account" in new EgraphsTestApplication {
    savedAccountWithEmailAndPassword(TestData.defaultPassword).password should not be (None)
  }

  "An Account" should "have different hashes and salts when the same password is set twice" in new EgraphsTestApplication {
    val credential = savedAccountWithEmailAndPassword(TestData.defaultPassword)
    val firstPassword = credential.password.get

    val password = credential.withPassword(TestData.defaultPassword).right.get.password.get

    firstPassword.is(TestData.defaultPassword) should be(true)
    password.is(TestData.defaultPassword) should be(true)
    password.hash should not be (firstPassword.hash)
    password.salt should not be (firstPassword.salt)
  }

  it should "fail validation for password lengths shorter than 8 characters" in new EgraphsTestApplication {
    for (password <- List("", "egraphs")) {
      val errorOrCredential = Account().withPassword(password)
      errorOrCredential.isLeft should be(true)
    }
  }

  it should "pass validation for password lengths 8 characters and longer" in new EgraphsTestApplication {
    Account().withPassword(TestData.defaultPassword).isRight should be(true)
  }

  it should "fail to authenticate with an AccountCredentialsError if the password is wrong" in new EgraphsTestApplication {
    val account = savedAccountWithEmailAndPassword("supersecret")
    accountStore.authenticate(account.email, "superWRONG") match {
      case Left(correct: AccountCredentialsError) => // phew
      case anythingElse => fail(anythingElse + " should have been a credentials error")
    }
  }

  it should "fail to authenticate with an AccountPasswordNotSetError if the account lacks a password" in new EgraphsTestApplication {
    val account = Account(email = TestData.generateEmail()).save()
    account.password should be(None)
    accountStore.authenticate(account.email, "supersecret") match {
      case Left(correct: AccountPasswordNotSetError) => // phew
      case anythingElse => fail(anythingElse + " should have been an AccountPasswordNotSetError")
    }
  }

  it should "fail to authenticate with an AccountNotFoundError if an account with the given email didnt exist" in new EgraphsTestApplication {
    accountStore.authenticate(TestData.generateEmail(), "supersecret") match {
      case Left(correct: AccountNotFoundError) => // phew
      case anythingElse => fail(anythingElse + " should have been an AccountNotFoundError")
    }
  }

  "withResetPasswordKey" should "set resetPasswordKey to newly generated key" in new EgraphsTestApplication {
    val account = TestData.newSavedAccount()
    account.resetPasswordKey should be(None)
    val accountWithResetKey = account.withResetPasswordKey.save()
    accountWithResetKey.resetPasswordKey.get should not be(None)
    accountWithResetKey.resetPasswordKey.get.lastIndexOf('.') should not be (-1)
  }

  "verifyResetPasswordKey" should "return false if resetPasswordKey is None" in new EgraphsTestApplication {
    val account = TestData.newSavedAccount()
    account.verifyResetPasswordKey("whatever") should be(false)
    val accountWithResetKey = account.withResetPasswordKey.save()
    accountWithResetKey.verifyResetPasswordKey("whatever.") should be(false)
    accountWithResetKey.verifyResetPasswordKey(accountWithResetKey.resetPasswordKey.get) should be(true)
  }

  "verifyResetPasswordKey" should "return false if resetPasswordKey is expired" in new EgraphsTestApplication {
    val accountWithOriginalResetKey = TestData.newSavedAccount().withResetPasswordKey.save()
    val oldKey = accountWithOriginalResetKey.resetPasswordKey.get
    val newKey = oldKey.substring(0, oldKey.lastIndexOf(".") + 1) + System.currentTimeMillis
    val accountWithNewResetKey = accountWithOriginalResetKey.copy(resetPasswordKey = Some(newKey)).save()
    accountWithNewResetKey.verifyResetPasswordKey(accountWithOriginalResetKey.resetPasswordKey.get) should be(false)
  }

  "withPassword" should "set resetPasswordKey to None" in new EgraphsTestApplication {
    val account = TestData.newSavedAccount().withResetPasswordKey.save()
    val accountWithNewPassword = account.withPassword("newpassword").right.get.save()
    accountWithNewPassword.resetPasswordKey should be(None)
  }

  "An account" should "save email in lowercase" in new EgraphsTestApplication {
    val stored = TestData.newSavedAccount().copy(email = TestData.generateEmail().toUpperCase).save()
    accountStore.findByEmail(stored.email.toLowerCase) should be(Some(stored.copy(email = stored.email.toLowerCase)))
  }

  it should "save email trimmed" in new EgraphsTestApplication {
    val stored = TestData.newSavedAccount()
      .copy(email =  "                    " + TestData.generateEmail() + "                    ").save()
    accountStore.findByEmail(stored.email.trim) should be(Some(stored))
  }

  "createCustomer" should "create Customer with username based on email" in new EgraphsTestApplication {
    val account = TestData.newSavedAccount()
    val customer = account.createCustomer("Test User").save()
    account.email should include(customer.username)
    //TODO: SER-223 should update this test to use the new table.
  }

  "createCustomer" should "throw exception if called on account that already has a customer" in new EgraphsTestApplication {
    val customer = TestData.newSavedCustomer()
    intercept[IllegalArgumentException] {customer.account.createCustomer("Another User")}
  }
}

class AccountStoreTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityIdLongTests[Account]
  with CreatedUpdatedEntityTests[Long, Account]
  with DateShouldMatchers
  with DBTransactionPerTest
  with AccountTestHelpers
{
  def celebrityStore = AppConfig.instance[CelebrityStore]
  def customerStore = AppConfig.instance[CustomerStore]
  def administratorStore = AppConfig.instance[AdministratorStore]

  def accountStore = AppConfig.instance[AccountStore]

  //
  // SavingEntityTests[Account] methods
  //
  override def newEntity = {
    Account(email = TestData.generateEmail())
  }

  override def saveEntity(toSave: Account) = {
    toSave.save()
  }

  override def restoreEntity(accountId: Long) = {
    accountStore.findById(accountId)
  }

  override def transformEntity(toTransform: Account) = {
    toTransform.copy(
      email = TestData.generateEmail(),
      passwordHash = Some(TestData.defaultPassword),
      passwordSalt = Some(TestData.defaultPassword),
      celebrityId = Some(Celebrity(publicName = TestData.generateFullname()).save().id),
      customerId = Some(Customer(name = "name", username = TestData.generateUsername()).save().id),
      administratorId = Some(Administrator().save().id)
    )
  }

  it should "store and retrieve correctly" in new EgraphsTestApplication {
    // Set up
    val stored = savedAccountWithEmailAndPassword(TestData.defaultPassword)
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
  
  it should "find by customer ID correctly" in new EgraphsTestApplication {
    val customer = TestData.newSavedCustomer()
    val account = customer.account
    accountStore.findByCustomerId(customer.id) should be (Some(account))
    accountStore.findByCustomerId(customer.id + 1) should be (None)
  }

  it should "should persist fine with no celebrity/customer/admin IDs" in new EgraphsTestApplication {
    Account(email = TestData.generateEmail()).save() // Doesn't throw any errors
  }

  it should "fail to persist with non-null, non-existent celebrity ID" in new EgraphsTestApplication {
    val thrown = evaluating {
                              accountStore.save(Account(celebrityId = Some(1L)))
                            } should produce[RuntimeException]
    thrown.getMessage.toUpperCase should include("CELEBRITYID")
  }

  it should "fail to persist with non-null, non-existent customer ID" in new EgraphsTestApplication {
    val thrown = evaluating {
                              accountStore.save(Account(customerId = Some(1L)))
                            } should produce[RuntimeException]
    thrown.getMessage.toUpperCase should include("CUSTOMERID")
  }

  it should "fail to persist with non-null, non-existent administrator ID" in new EgraphsTestApplication {
    val thrown = evaluating {
                              accountStore.save(Account(administratorId = Some(1L)))
                            } should produce[RuntimeException]
    thrown.getMessage.toUpperCase should include("ADMINISTRATORID")
  }

  it should "be recoverable by email" in new EgraphsTestApplication {
    accountStore.findByEmail(null) should be(None)
    accountStore.findByEmail("") should be(None)
    val stored = Account(email = TestData.generateEmail()).save()
    accountStore.findByEmail(stored.email) should be(Some(stored))
  }

  it should "authenticate the correct email and password in" in new EgraphsTestApplication {
    val stored = savedAccountWithEmailAndPassword("supersecret")
    accountStore.authenticate(stored.email, "supersecret") should be(Right(stored))
  }

}

trait AccountTestHelpers {
  def accountStore: AccountStore

  def savedAccountWithEmailAndPassword(password: String): Account = {
    TestData.newSavedAccount().withPassword(password).right.get.save()
  }
}