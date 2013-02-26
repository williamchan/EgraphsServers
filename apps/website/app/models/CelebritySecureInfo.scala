package models

import java.sql.Timestamp
import com.google.inject.Inject
import models.enums.BankAccountType
import models.enums.HasDepositAccountType
import services.db._
import services.Time
import services.AppConfig

/**
 * Services used by each celebrity secure info instance
 */
case class CelebritySecureInfoServices @Inject() (
  store: CelebritySecureInfoStore,
  schema: Schema
)

case class DecryptedCelebritySecureInfo(
  id: Long = 0,
  smsPhone: Option[String] = None,
  voicePhone: Option[String] = None,
  agentEmail: Option[String] = None,
  streetAddress: Option[String] = None,
  city: Option[String] = None,
  postalCode: Option[String] = None,
  country: Option[String] = None,
  _depositAccountType: Option[String] = None,
  depositAccountRoutingNumber: Option[Int] = None,
  depositAccountNumber: Option[Int] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: CelebritySecureInfoServices = AppConfig.instance[CelebritySecureInfoServices]
) extends CelebritySecureInfo
  with HasCreatedUpdated
  with HasDepositAccountType[DecryptedCelebritySecureInfo]
{
  def encrypted = EncryptedCelebritySecureInfo(
    id = id,
    smsPhone = smsPhone.map(encrypt(_)),
    voicePhone = voicePhone.map(encrypt(_)),
    agentEmail = agentEmail.map(encrypt(_)),
    streetAddress = streetAddress.map(encrypt(_)),
    city = city.map(encrypt(_)),
    postalCode = postalCode.map(encrypt(_)),
    country = country.map(encrypt(_)),
    _depositAccountType = _depositAccountType,
    depositAccountRoutingNumber = depositAccountRoutingNumber.map(encrypt(_)),
    depositAccountNumber = depositAccountNumber.map(encrypt(_)),
    created = created,
    updated = updated
  )

  override def withDepositAccountType(depositAccountType: BankAccountType.EnumVal) = {
    this.copy(_depositAccountType = Some(depositAccountType.name))
  }
}

case class EncryptedCelebritySecureInfo(
  id: Long = 0,
  smsPhone: Option[String] = None,
  voicePhone: Option[String] = None,
  agentEmail: Option[String] = None,
  streetAddress: Option[String] = None,
  city: Option[String] = None,
  postalCode: Option[String] = None,
  country: Option[String] = None,
  _depositAccountType: Option[String] = None,
  depositAccountRoutingNumber: Option[Int] = None,
  depositAccountNumber: Option[Int] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: CelebritySecureInfoServices = AppConfig.instance[CelebritySecureInfoServices]
) extends CelebritySecureInfo
  with KeyedCaseClass[Long]
  with HasCreatedUpdated
  with HasDepositAccountType[EncryptedCelebritySecureInfo]
{
  def decrypted = DecryptedCelebritySecureInfo(
    id = id,
    smsPhone = smsPhone.map(decrypt(_)),
    voicePhone = voicePhone.map(decrypt(_)),
    agentEmail = agentEmail.map(decrypt(_)),
    streetAddress = streetAddress.map(decrypt(_)),
    city = city.map(decrypt(_)),
    postalCode = postalCode.map(decrypt(_)),
    country = country.map(decrypt(_)),
    _depositAccountType = _depositAccountType,
    depositAccountRoutingNumber = depositAccountRoutingNumber.map(decrypt(_)),
    depositAccountNumber = depositAccountNumber.map(decrypt(_)),
    created = created,
    updated = updated
  )

  override def withDepositAccountType(depositAccountType: BankAccountType.EnumVal) = {
    this.copy(_depositAccountType = Some(depositAccountType.name))
  }

  //
  // KeyedCaseClass[Long] methods
  //
  override def unapplied = EncryptedCelebritySecureInfo.unapply(this)
}

abstract class CelebritySecureInfo {
  def id: Long
  def smsPhone: Option[String]
  def voicePhone: Option[String]
  def agentEmail: Option[String]
  def streetAddress: Option[String]
  def city: Option[String] = None
  def postalCode: Option[String]
  def country: Option[String]
  def _depositAccountType: Option[String]
  def depositAccountRoutingNumber: Option[Int]
  def depositAccountNumber: Option[Int]
  def created: Timestamp
  def updated: Timestamp
  
  def hasAllDepositInformation: Boolean = {
    val maybeAllDepositInfo = Set(
      streetAddress,
      city,
      postalCode,
      country,
      _depositAccountType,
      depositAccountRoutingNumber,
      depositAccountNumber
    )
    maybeAllDepositInfo.filter(_.isDefined) == maybeAllDepositInfo
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