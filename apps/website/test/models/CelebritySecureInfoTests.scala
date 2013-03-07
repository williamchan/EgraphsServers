package models

import utils._
import services.AppConfig
import org.apache.commons.lang3.RandomStringUtils
import models.enums.BankAccountType
import scala.util.Random

class CelebritySecureInfoTests extends EgraphsUnitTest
  with SavingEntityIdLongTests[EncryptedCelebritySecureInfo]
  with CreatedUpdatedEntityTests[Long, EncryptedCelebritySecureInfo]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  def store = AppConfig.instance[CelebritySecureInfoStore]

  "CelebritySecureInfo.anonymize" should "anonymize strings" in {
    val x = CelebritySecureInfo.anonymized("123456789", 3)
    x should be ("******789")
  }

  "encrypt/decrypt" should "be able to arrive at the same thing" in {
    val decrypted = DecryptedCelebritySecureInfo(
      contactEmail = Some(TestData.generateEmail("bezos", "clamazon.com")),
      smsPhone = Some(RandomStringUtils.randomNumeric(10)),
      voicePhone = Some(RandomStringUtils.randomNumeric(10)),
      agentEmail = Some(TestData.generateEmail("bezosAgent", "clamazon.com")),
      streetAddress = Some(RandomStringUtils.randomAlphanumeric(40)),
      city = Some(RandomStringUtils.randomAlphanumeric(40)),
      state = Some("QC"),
      postalCode = Some("ABC 123"),
      country = Some("CA")
    ).withDepositAccountType(Some(BankAccountType.Checking))
    .withDepositAccountRoutingNumber(Some(RandomStringUtils.randomNumeric(9).toInt))
    .withDepositAccountNumber(Some(Random.nextLong.abs))
    
    val encrypted = decrypted.encrypt

    decrypted should not be (encrypted)
    encrypted.decrypt should be (decrypted)
  }

  "numberOfContactMethods" should "return the number of contact methods filled in" in {
    val secureInfo = DecryptedCelebritySecureInfo(
      contactEmail = Some(TestData.generateEmail("myyk", "clamazon.com")),
      smsPhone = Some(RandomStringUtils.randomNumeric(10)),
      voicePhone = Some(RandomStringUtils.randomNumeric(10)),
      agentEmail = Some(TestData.generateEmail("myyksAgent", "clamazon.com"))
    )
    
    secureInfo.numberOfContactMethods should be (4)
    secureInfo.encrypt.numberOfContactMethods should be (4)

    val lessInfo = secureInfo.copy(contactEmail = None, agentEmail = None)
    lessInfo.numberOfContactMethods should be (2)
    lessInfo.encrypt.numberOfContactMethods should be (2)

    val noInfo = DecryptedCelebritySecureInfo()
    noInfo.numberOfContactMethods should be (0)
    noInfo.encrypt.numberOfContactMethods should be (0)
  }

  "hasAllDepositInformation" should "return true only if all information for a direct deposit is filled in" in {
    val allFilledIn = DecryptedCelebritySecureInfo(
      streetAddress = Some(RandomStringUtils.randomAlphanumeric(40)),
      city = Some(RandomStringUtils.randomAlphanumeric(40)),
      state = Some("QC"),
      postalCode = Some("ABC 123"),
      country = Some("CA")
    ).withDepositAccountType(Some(BankAccountType.Checking))
    .withDepositAccountRoutingNumber(Some(RandomStringUtils.randomNumeric(9).toInt))
    .withDepositAccountNumber(Some(Random.nextLong.abs))

    allFilledIn.hasAllDepositInformation should be (true)
    allFilledIn.encrypt.hasAllDepositInformation should be (true)

    val someFilledIn = allFilledIn.withDepositAccountType(None)
    someFilledIn.hasAllDepositInformation should be (false)
    someFilledIn.encrypt.hasAllDepositInformation should be (false)
  }

  "anonymizedDepositAccountRoutingNumber" should "be anonymized" in {
    val secureInfo = DecryptedCelebritySecureInfo().withDepositAccountRoutingNumber(Some(Random.nextInt.abs))
    secureInfo.anonymizedDepositAccountRoutingNumber should not be (secureInfo._depositAccountRoutingNumber.toString)
  }

  "anonymizedDepositAccountNumber" should "be anonymized" in {
    val secureInfo = DecryptedCelebritySecureInfo().withDepositAccountNumber(Some(Random.nextLong.abs))
    secureInfo.anonymizedDepositAccountNumber should not be (secureInfo._depositAccountNumber.toString)
  }

  //
  // SavingEntityTests[EncryptedCelebritySecureInfo] methods
  //
  override def newEntity = {
    EncryptedCelebritySecureInfo()
  }

  override def saveEntity(toSave: EncryptedCelebritySecureInfo) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: EncryptedCelebritySecureInfo) = {
    toTransform.decrypt.copy(
      contactEmail = Some(TestData.generateEmail("myyk", "clamazon.com")),
      smsPhone = Some(RandomStringUtils.randomNumeric(10)),
      voicePhone = Some(RandomStringUtils.randomNumeric(10)),
      agentEmail = Some(TestData.generateEmail("myyksAgent", "clamazon.com")),
      streetAddress = Some(RandomStringUtils.randomAlphanumeric(40)),
      city = Some(RandomStringUtils.randomAlphanumeric(40)),
      state = Some("QC"),
      postalCode = Some("ABC 123"),
      country = Some("CA")
    ).withDepositAccountType(Some(BankAccountType.Checking))
    .withDepositAccountRoutingNumber(Some(RandomStringUtils.randomNumeric(9).toInt))
    .withDepositAccountNumber(Some(Random.nextLong.abs))
    .encrypt
  }
}