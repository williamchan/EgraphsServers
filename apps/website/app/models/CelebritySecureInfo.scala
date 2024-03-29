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
  id: Option[Long],
  accountSettingsComplete: Option[Boolean],
  twitterUsername: Option[String],
  contactEmail: Option[EmailAddress],
  smsPhone: Option[String],
  voicePhone: Option[String],
  agentEmail: Option[EmailAddress]
)

// TODO: After Play 2.1.1+ delete the extends FunctionX, for more info see https://groups.google.com/forum/#!topic/play-framework/ENlcpDzLZo8/discussion and https://groups.google.com/forum/?fromgroups=#!topic/play-framework/1u6IKEmSRqY
object JsCelebrityContactInfo extends Function7[Option[Long], Option[Boolean], Option[String], Option[EmailAddress], Option[String], Option[String], Option[EmailAddress], JsCelebrityContactInfo] {
  implicit val celebrityContactInfoFormats = Json.format[JsCelebrityContactInfo]

  def from(celebrity: Celebrity): JsCelebrityContactInfo = {
    from(celebrity, celebrity.secureInfo)
  }

  def from(celebrity: Celebrity, secureInfo: Option[DecryptedCelebritySecureInfo]): JsCelebrityContactInfo = {
    JsCelebrityContactInfo(
      id = Some(celebrity.id),
      accountSettingsComplete = Some(celebrity.isAccountSettingsComplete(secureInfo)),
      twitterUsername = celebrity.twitterUsername,
      contactEmail = secureInfo.flatMap(info => info.contactEmail.map(EmailAddress(_))),
      smsPhone = secureInfo.flatMap(_.smsPhone),
      voicePhone = secureInfo.flatMap(_.voicePhone),
      agentEmail = secureInfo.flatMap(info => info.agentEmail.map(EmailAddress(_)))
    )
  }
}

case class JsCelebrityDepositInfo(
  id: Option[Long],
  accountSettingsComplete: Option[Boolean],
  addressLine1: Option[String],
  addressLine2: Option[String],
  city: Option[String],
  state: Option[String],
  postalCode: Option[String],
  countryCode: Option[String],
  depositAccountType: Option[BankAccountType.EnumVal],
  depositAccountRoutingNumber: Option[Int] = None,
  depositAccountNumber: Option[Long] = None,
  isDepositAccountChange: Option[Boolean] = None,
  anonymousDepositAccountRoutingNumber: Option[String] = None,
  anonymousDepositAccountNumber: Option[String] = None
)

// TODO: After Play 2.1.1+ delete the extends FunctionX, for more info see https://groups.google.com/forum/#!topic/play-framework/ENlcpDzLZo8/discussion and https://groups.google.com/forum/?fromgroups=#!topic/play-framework/1u6IKEmSRqY
object JsCelebrityDepositInfo extends Function14[Option[Long], Option[Boolean], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[BankAccountType.EnumVal], Option[Int], Option[Long], Option[Boolean], Option[String], Option[String], JsCelebrityDepositInfo] {
  implicit val celebrityDepositInfoFormats = Json.format[JsCelebrityDepositInfo]

  def from(celebrity: Celebrity): JsCelebrityDepositInfo = {
    from(celebrity, celebrity.secureInfo)
  }

  def from(celebrity: Celebrity, secureInfo: Option[DecryptedCelebritySecureInfo]): JsCelebrityDepositInfo = {
    JsCelebrityDepositInfo(
      id = Some(celebrity.id),
      accountSettingsComplete = Some(celebrity.isAccountSettingsComplete(secureInfo)),
      addressLine1 = secureInfo.flatMap(_.addressLine1),
      addressLine2 = secureInfo.flatMap(_.addressLine2),
      city = secureInfo.flatMap(_.city),
      state = secureInfo.flatMap(_.state),
      postalCode = secureInfo.flatMap(_.postalCode),
      countryCode = secureInfo.flatMap(_.countryCode),
      depositAccountType = secureInfo.flatMap(info => info.depositAccountType),
      anonymousDepositAccountRoutingNumber = secureInfo.flatMap(_.anonymizedDepositAccountRoutingNumber),
      anonymousDepositAccountNumber = secureInfo.flatMap(_.anonymizedDepositAccountNumber)
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
  addressLine1: Option[String] = None,
  addressLine2: Option[String] = None,
  city: Option[String] = None,
  state: Option[String] = None,
  postalCode: Option[String] = None,
  countryCode: Option[String] = None,
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
    addressLine1 = addressLine1.map(encryptAES(_)),
    addressLine2 = addressLine2.map(encryptAES(_)),
    city = city.map(encryptAES(_)),
    state = state.map(encryptAES(_)),
    postalCode = postalCode.map(encryptAES(_)),
    countryCode = countryCode.map(encryptAES(_)),
    _depositAccountType = _depositAccountType,
    _depositAccountRoutingNumber = _depositAccountRoutingNumber.map(encryptAES(_)),
    _depositAccountNumber = _depositAccountNumber.map(encryptAES(_)),
    created = created,
    updated = updated
  )

  def updateFromContactInfo(contactInfo: JsCelebrityContactInfo): DecryptedCelebritySecureInfo = {
    copy(
      contactEmail = contactInfo.contactEmail.map(_.value),
      smsPhone = contactInfo.smsPhone,
      voicePhone = contactInfo.voicePhone,
      agentEmail = contactInfo.agentEmail.map(_.value)
    )
  }

  def updateFromDepositInfo(depositInfo: JsCelebrityDepositInfo): DecryptedCelebritySecureInfo = {
    val partialSecureInfo = copy(
      addressLine1 = depositInfo.addressLine1,
      addressLine2 = depositInfo.addressLine2,
      city = depositInfo.city,
      postalCode = depositInfo.postalCode,
      state = depositInfo.state,
      countryCode = depositInfo.countryCode
    )

    if (depositInfo.isDepositAccountChange.get) {
      partialSecureInfo.withDepositAccountType(depositInfo.depositAccountType)
        .withDepositAccountRoutingNumber(depositInfo.depositAccountRoutingNumber)
        .withDepositAccountNumber(depositInfo.depositAccountNumber)
    } else partialSecureInfo
  }

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
  addressLine1: Option[String] = None,
  addressLine2: Option[String] = None,
  city: Option[String] = None,
  state: Option[String] = None,
  postalCode: Option[String] = None,
  countryCode: Option[String] = None,
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
    addressLine1 = addressLine1.map(decryptAES(_)),
    addressLine2 = addressLine2.map(decryptAES(_)),
    city = city.map(decryptAES(_)),
    state = state.map(decryptAES(_)),
    postalCode = postalCode.map(decryptAES(_)),
    countryCode = countryCode.map(decryptAES(_)),
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
  def addressLine1: Option[String]
  def addressLine2: Option[String]
  def city: Option[String]
  def state: Option[String]
  def postalCode: Option[String]
  def countryCode: Option[String]
  def _depositAccountType: Option[String]
  def _depositAccountRoutingNumber: Option[String]
  def _depositAccountNumber: Option[String]
  def created: Timestamp
  def updated: Timestamp

  def numberOfContactMethods: Int = {
    val maybeContactMethods = List(contactEmail, smsPhone, voicePhone, agentEmail)
    maybeContactMethods.flatten.size
  }

  def hasAllDepositInformation: Boolean = {
    val maybeAllDepositInfo = Set(
      addressLine1,
      city,
      state,
      postalCode,
      countryCode,
      _depositAccountType,
      _depositAccountRoutingNumber,
      _depositAccountNumber
    )
    maybeAllDepositInfo.forall(_.isDefined)
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