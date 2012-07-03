package models

import play.data.validation.Validation.ValidationResult
import java.sql.Timestamp
import services.db.{KeyedCaseClass, Saves, Schema}
import com.google.inject.Inject
import java.util.UUID
import org.apache.commons.codec.binary.Base64
import services.{Utils, AppConfig, Time}

/**
 * Basic account information for any user in the system
 */
case class Account(
  id: Long = 0,
  email: String = "",
  passwordHash: Option[String] = None,
  passwordSalt: Option[String] = None,
  emailVerified: Boolean = false,
  resetPasswordKey: Option[String] = None,
  fbUserId: Option[String] = None,
  customerId: Option[Long] = None,
  celebrityId: Option[Long] = None,
  administratorId: Option[Long] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: AccountServices = AppConfig.instance[AccountServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  def save(): Account = {
    require(!email.isEmpty, "Account: email must be specified")
    services.accountStore.save(this)
  }

  /**
   * Creates a Customer with a default username based on the Account's email. Throws an exception if customerId is defined.
   *
   * @param name name of Customer
   * @return newly created Customer
   */
  def createCustomer(name: String): Customer = {
    require(customerId.isEmpty, "Cannot create Customer on Account that already has one")
    Customer(name = name, username = email.split("@").head)
  }

  def password: Option[Password] = {
    (passwordHash, passwordSalt) match {
      case (Some(""), Some("")) => None
      case (Some(hash), Some(salt)) => Some(Password(hash, salt))
      case _ => None
    }
  }

  def emailVerify() : Account = {
    copy(emailVerified = true)
  }

  /**
   * Sets a new password onto the entity. Also, sets resetPasswordKey to None, thereby expiring it.
   *
   * @param newPassword the password to set onto the returned element empty passwords or
   *    passwords fewer than 4 characters are invalid.
   *
   * @return either a credential with the given password (right), or the erroneous validation
   *    result against the provided entity (left).
   */
  def withPassword(newPassword: String): Either[ValidationResult, Account] = {
    Password.validate(newPassword) match {
      case Left(validationResult) => Left(validationResult)
      case _ => {
        val password = Password(newPassword, id)
        Right(copy(passwordHash = Some(password.hash),
          passwordSalt = Some(password.salt),
          resetPasswordKey = None))
      }
    }
  }

  /**
   * @return an account with resetPasswordKey set to a newly generated value
   */
  def withResetPasswordKey: Account = {
    val uuid = UUID.randomUUID()
    val expirationTime = System.currentTimeMillis + Time.millisInDay
    val key = Base64.encodeBase64URLSafeString(uuid.toString.getBytes) + '.' + expirationTime
    copy(resetPasswordKey = Some(key))
  }

  /**
   * @param attempt the key to verify
   * @return whether attempt matches non-expired resetPasswordKey, or false if resetPasswordKey is None
   */
  def verifyResetPasswordKey(attempt: String): Boolean = {
    if (resetPasswordKey.isEmpty) return false
    val indexOfExpirationTime = attempt.lastIndexOf(".") + 1
    if (indexOfExpirationTime <= 0) return false

    // This guards against malformed attempt Strings
    val expirationTime = try {
      attempt.substring(indexOfExpirationTime).toLong
    } catch {
      case _ => return false
    }

    (resetPasswordKey.get == attempt) && (System.currentTimeMillis < expirationTime)
  }

  def addresses = {
    services.addressStore.findByAccount(id)
  }

  //
  // KeyedCaseClass methods
  //
  override def unapplied = Account.unapply(this)
}

object Account {
  val minPasswordLength = 8
}

/**
 * Services used by each Account object
 */
case class AccountServices @Inject() (accountStore: AccountStore, customerStore: CustomerStore, addressStore: AddressStore)

class AccountStore @Inject() (schema: Schema) extends Saves[Account] with SavesCreatedUpdated[Account] {
  import org.squeryl.PrimitiveTypeMode._

  def authenticate(email: String, passwordAttempt: String): Either[AccountAuthenticationError, Account] = {
    import AccountAuthenticationError._

    findByEmail(email) match {
      case None =>
        Left(new AccountNotFoundError)

      case Some(account) =>
        account.password match {
          case None =>
            Left(new AccountPasswordNotSetError)

          case Some(password) if password.is(passwordAttempt) =>
            Right(account)

          case _ =>
            Left(new AccountCredentialsError)
        }
    }
  }

  //
  // Public methods
  //
  def findByEmail(email: String): Option[Account] = {
    Utils.toOption(email) match {
      case None => None
      case Some(e) => {
        val emailInLowerCase = e.trim().toLowerCase
        from(schema.accounts)(account =>
          where(account.email === emailInLowerCase)
            select (account)
        ).headOption
      }
    }
  }

  def findByAdministratorId(administratorId: Long): Option[Account] = {
    from(schema.accounts)(account =>
      where(account.administratorId === administratorId)
        select (account)
    ).headOption
  }

  def findByCelebrityId(celebrityId: Long): Option[Account] = {
    from(schema.accounts)(account =>
      where(account.celebrityId === celebrityId)
        select (account)
    ).headOption
  }

  def findByCustomerId(customerId: Long): Option[Account] = {
    from(schema.accounts)(account =>
      where(account.customerId === customerId)
        select (account)
    ).headOption
  }

  //
  // Saves[Account] methods
  //
  override val table = schema.accounts

  override def defineUpdate(theOld: Account, theNew: Account) = {
    updateIs(
      theOld.email := theNew.email,
      theOld.passwordHash := theNew.passwordHash,
      theOld.passwordSalt := theNew.passwordSalt,
      theOld.resetPasswordKey := theNew.resetPasswordKey,
      theOld.fbUserId := theNew.fbUserId,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated,
      theOld.celebrityId := theNew.celebrityId,
      theOld.customerId := theNew.customerId,
      theOld.administratorId := theNew.administratorId,
      theOld.emailVerified := theNew.emailVerified
    )
  }

  beforeInsert(withEmailInLowerCase)
  beforeUpdate(withEmailInLowerCase)

  private def withEmailInLowerCase(toUpdate: Account): Account = {
    toUpdate.copy(email = toUpdate.email.trim().toLowerCase)
  }

  //
  // SavesCreatedUpdated[Account] methods
  //
  override protected def withCreatedUpdated(toUpdate: Account,
                                            created: Timestamp,
                                            updated: Timestamp): Account = {
    toUpdate.copy(created = created, updated = updated)
  }
}

// Errors
sealed class AccountAuthenticationError

object AccountAuthenticationError {
  class AccountCredentialsError extends AccountAuthenticationError
  class AccountPasswordNotSetError extends AccountAuthenticationError
  class AccountNotFoundError extends AccountAuthenticationError
  class AccountNotVerifiedError extends AccountAuthenticationError
}
