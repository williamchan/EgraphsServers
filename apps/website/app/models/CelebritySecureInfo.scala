package models

import java.sql.Timestamp
import com.google.inject.Inject
import play.api.libs.Crypto._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.enums.BankAccountType
import models.enums.HasDepositAccountType
import services.db._
import services.Time
import services.AppConfig

case class JsCelebrityContactInfo(
  id: Long,
  accountSettingsComplete: Boolean,
  twitterUsername: Option[String],
  contactEmail: Option[EmailAddress],
  smsPhone: Option[String],
  voicePhone: Option[String],
  agentEmail: Option[EmailAddress]
)

// TODO: After Play 2.1.1+ delete the extends FunctionX, for more info see https://groups.google.com/forum/#!topic/play-framework/ENlcpDzLZo8/discussion and https://groups.google.com/forum/?fromgroups=#!topic/play-framework/1u6IKEmSRqY
object JsCelebrityContactInfo extends Function7[Long, Boolean, Option[String], Option[EmailAddress], Option[String], Option[String], Option[EmailAddress], JsCelebrityContactInfo] {
  implicit val celebrityContactInfoFormats = Json.format[JsCelebrityContactInfo]

  def from(celebrity: Celebrity): JsCelebrityContactInfo = {
    from(celebrity, celebrity.secureInfo)
  }

  def from(celebrity: Celebrity, secureInfo: Option[DecryptedCelebritySecureInfo]): JsCelebrityContactInfo = {
    JsCelebrityContactInfo(
      id = celebrity.id,
      accountSettingsComplete = celebrity.isAccountSettingsComplete(secureInfo),
      twitterUsername = celebrity.twitterUsername,
      contactEmail = secureInfo.map(info => info.contactEmail.map(EmailAddress(_))).flatten,
      smsPhone = secureInfo.map(_.smsPhone).flatten,
      voicePhone = secureInfo.map(_.voicePhone).flatten,
      agentEmail = secureInfo.map(info => info.agentEmail.map(EmailAddress(_))).flatten
    )
  }
}

case class JsCelebrityDepositInfo(
  id: Long,
  accountSettingsComplete: Boolean,
  streetAddress: Option[String],
  city: Option[String],
  postalCode: Option[String],
  country: Option[String],
  depositAccountType: Option[BankAccountType.EnumVal],
  depositAccountRoutingNumber: Option[Int] = None,
  depositAccountNumber: Option[Long] = None,
  isDepositAccountChange: Option[Boolean] = None,
  anonymousDepositAccountRoutingNumber: Option[String] = None,
  anonymousDepositAccountNumber: Option[String] = None
)

// TODO: After Play 2.1.1+ delete the extends FunctionX, for more info see https://groups.google.com/forum/#!topic/play-framework/ENlcpDzLZo8/discussion and https://groups.google.com/forum/?fromgroups=#!topic/play-framework/1u6IKEmSRqY
object JsCelebrityDepositInfo extends Function12[Long, Boolean, Option[String], Option[String], Option[String], Option[String], Option[BankAccountType.EnumVal], Option[Int], Option[Long], Option[Boolean], Option[String], Option[String], JsCelebrityDepositInfo] {
  implicit val celebrityDepositInfoFormats = Json.format[JsCelebrityDepositInfo]

  def from(celebrity: Celebrity): JsCelebrityDepositInfo = {
    from(celebrity, celebrity.secureInfo)
  }

  def from(celebrity: Celebrity, secureInfo: Option[DecryptedCelebritySecureInfo]): JsCelebrityDepositInfo = {
    JsCelebrityDepositInfo(
      id = celebrity.id,
      accountSettingsComplete = celebrity.isAccountSettingsComplete(secureInfo),
      streetAddress = secureInfo.map(_.streetAddress).flatten,
      city = secureInfo.map(_.city).flatten,
      postalCode = secureInfo.map(_.postalCode).flatten,
      country = secureInfo.map(_.country).flatten,
      depositAccountType = secureInfo.map(info => info.depositAccountType).flatten,
      anonymousDepositAccountRoutingNumber = secureInfo.map(_.anonymizedDepositAccountRoutingNumber).flatten,
      anonymousDepositAccountNumber = secureInfo.map(_.anonymizedDepositAccountNumber).flatten
    )
  }
}

/**
 * Services used by each celebrity secure info instance
 */
case class CelebritySecureInfoServices @Inject() (
  store: CelebritySecureInfoStore,
  schema: Schema
)

case class DecryptedCelebritySecureInfo(
  id: Long = 0,
  contactEmail: Option[String] = None,
  smsPhone: Option[String] = None,
  voicePhone: Option[String] = None,
  agentEmail: Option[String] = None,
  streetAddress: Option[String] = None,
  city: Option[String] = None,
  postalCode: Option[String] = None,
  country: Option[String] = None,
  _depositAccountType: Option[String] = None,
  _depositAccountRoutingNumber: Option[String] = None,
  _depositAccountNumber: Option[String] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: CelebritySecureInfoServices = AppConfig.instance[CelebritySecureInfoServices]
) extends CelebritySecureInfo
  with HasCreatedUpdated
  with HasDepositAccountType[DecryptedCelebritySecureInfo]
{
  lazy val depositAccountRoutingNumber: Option[Int] = _depositAccountRoutingNumber.map(_.toInt)
  lazy val depositAccountNumber: Option[Long] = _depositAccountNumber.map(_.toLong)

  def anonymizedDepositAccountRoutingNumber: Option[String] = _depositAccountRoutingNumber.map(CelebritySecureInfo.anonymized(_, 4))
  def anonymizedDepositAccountNumber: Option[String] = _depositAccountNumber.map(CelebritySecureInfo.anonymized(_, 4))

  def withDepositAccountRoutingNumber(maybeRoutingNumber: Option[Int]): DecryptedCelebritySecureInfo = copy(_depositAccountRoutingNumber = maybeRoutingNumber.map(_.toString))
  def withDepositAccountNumber(maybeAccountNumber: Option[Long]): DecryptedCelebritySecureInfo = copy(_depositAccountNumber = maybeAccountNumber.map(_.toString))

  def encrypt = EncryptedCelebritySecureInfo(
    id = id,
    contactEmail = contactEmail.map(encryptAES(_)),
    smsPhone = smsPhone.map(encryptAES(_)),
    voicePhone = voicePhone.map(encryptAES(_)),
    agentEmail = agentEmail.map(encryptAES(_)),
    streetAddress = streetAddress.map(encryptAES(_)),
    city = city.map(encryptAES(_)),
    postalCode = postalCode.map(encryptAES(_)),
    country = country.map(encryptAES(_)),
    _depositAccountType = _depositAccountType,
    _depositAccountRoutingNumber = _depositAccountRoutingNumber.map(encryptAES(_)),
    _depositAccountNumber = _depositAccountNumber.map(encryptAES(_)),
    created = created,
    updated = updated
  )

  override def withDepositAccountType(maybeDepositAccountType: Option[BankAccountType.EnumVal]) = {
    this.copy(_depositAccountType = maybeDepositAccountType.map(_.name))
  }
}

case class EncryptedCelebritySecureInfo(
  id: Long = 0,
  contactEmail: Option[String] = None,
  smsPhone: Option[String] = None,
  voicePhone: Option[String] = None,
  agentEmail: Option[String] = None,
  streetAddress: Option[String] = None,
  city: Option[String] = None,
  postalCode: Option[String] = None,
  country: Option[String] = None,
  _depositAccountType: Option[String] = None,
  _depositAccountRoutingNumber: Option[String] = None,
  _depositAccountNumber: Option[String] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: CelebritySecureInfoServices = AppConfig.instance[CelebritySecureInfoServices]
) extends CelebritySecureInfo
  with KeyedCaseClass[Long]
  with HasCreatedUpdated
  with HasDepositAccountType[EncryptedCelebritySecureInfo]
{
  def save(): EncryptedCelebritySecureInfo = {
    services.store.save(this)
  }

  def decrypt = DecryptedCelebritySecureInfo(
    id = id,
    contactEmail = contactEmail.map(decryptAES(_)),
    smsPhone = smsPhone.map(decryptAES(_)),
    voicePhone = voicePhone.map(decryptAES(_)),
    agentEmail = agentEmail.map(decryptAES(_)),
    streetAddress = streetAddress.map(decryptAES(_)),
    city = city.map(decryptAES(_)),
    postalCode = postalCode.map(decryptAES(_)),
    country = country.map(decryptAES(_)),
    _depositAccountType = _depositAccountType,
    _depositAccountRoutingNumber = _depositAccountRoutingNumber.map(decryptAES(_).toInt.toString), // this tests the string is really an int.
    _depositAccountNumber = _depositAccountNumber.map(decryptAES(_).toLong.toString), // this tests the string is really a long.
    created = created,
    updated = updated
  )

  override def withDepositAccountType(maybeDepositAccountType: Option[BankAccountType.EnumVal]) = {
    this.copy(_depositAccountType = maybeDepositAccountType.map(_.name))
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = EncryptedCelebritySecureInfo.unapply(this)
}

abstract class CelebritySecureInfo {
  def id: Long
  def contactEmail: Option[String]
  def smsPhone: Option[String]
  def voicePhone: Option[String]
  def agentEmail: Option[String]
  def streetAddress: Option[String]
  def city: Option[String]
  def postalCode: Option[String]
  def country: Option[String]
  def _depositAccountType: Option[String]
  def _depositAccountRoutingNumber: Option[String]
  def _depositAccountNumber: Option[String]
  def created: Timestamp
  def updated: Timestamp

  def numberOfContactMethods: Int = {
    val maybeContactMethods = Set(contactEmail, smsPhone, voicePhone, agentEmail)
    maybeContactMethods.flatten.size
  }

  def hasAllDepositInformation: Boolean = {
    val maybeAllDepositInfo = Set(
      streetAddress,
      city,
      postalCode,
      country,
      _depositAccountType,
      _depositAccountRoutingNumber,
      _depositAccountNumber
    )
    maybeAllDepositInfo.filter(_.isDefined) == maybeAllDepositInfo
  }
}

object CelebritySecureInfo {
  /**
   * Takes a string and shows the last $takeRight characters and replaces
   * all other characters with '*'.
   *   For example:
   *     val x = anonymized("123456789", 3)
   *     x == "******789" // would be true
   */
  def anonymized(insecure: String, takeRight: Int): String = {
    val hint = insecure.takeRight(takeRight)
    ("*" * (insecure.size - hint.size)) + hint
  }
}

class CelebritySecureInfoStore @Inject() (
  schema: Schema,
  dbSession: DBSession) extends SavesWithLongKey[EncryptedCelebritySecureInfo] with SavesCreatedUpdated[EncryptedCelebritySecureInfo] {

  import org.squeryl.PrimitiveTypeMode._

  //
  // SavesWithLongKey[EncryptedCelebritySecureInfo] methods
  //
  override val table = schema.encryptedCelebritySecureInfos

  //
  // SavesCreatedUpdated[EncryptedCelebritySecureInfo] methods
  //
  override def withCreatedUpdated(toUpdate: EncryptedCelebritySecureInfo, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created = created, updated = updated)
  }
}