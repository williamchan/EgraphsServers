package models

import org.squeryl.PrimitiveTypeMode._
import play.data.validation.Validation.ValidationResult
import play.data.validation.Validation
import java.sql.Timestamp
import services.Time
import services.db.{KeyedCaseClass, Saves, Schema}
import com.google.inject.Inject
import services.AppConfig


/**
 * Services used by each Account object
 */
case class AccountServices @Inject() (accountStore: AccountStore)

/**
 * Basic account information for any user in the system
 */
case class Account(
  id: Long = 0,
  email: String = "",
  passwordHash: Option[String] = None,
  passwordSalt: Option[String] = None,
  customerId: Option[Long] = None,
  celebrityId: Option[Long] = None,
  administratorId: Option[Long] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: AccountServices = AppConfig.instance[AccountServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  def save(): Account = {
    services.accountStore.save(this)
  }

  def password: Option[Password] = {
    (passwordHash, passwordSalt) match {
      case (Some(""), Some("")) => None
      case (Some(hash), Some(salt)) => Some(Password(hash, salt))
      case _ => None
    }
  }

  /**
   * Sets a new password onto the entity.
   *
   * @param newPassword the password to set onto the returned element empty passwords or
   *    passwords fewer than 4 characters are invalid.
   *
   * @return either a credential with the given password (right), or the erroneous validation
   *    result against the provided entity (left).
   */
  def withPassword(newPassword: String): Either[ValidationResult, Account] = {
    // Perform checks
    val existsCheck = Validation.required("password", newPassword)
    val lengthCheck = Validation.minSize("password", newPassword, 4)

    (existsCheck.ok, lengthCheck.ok) match {
      case (false, _) => Left(existsCheck)
      case (_, false) => Left(lengthCheck)
      case (true, true) =>
        val password = Password(newPassword, id)
        Right(copy(passwordHash = Some(password.hash), passwordSalt = Some(password.salt)))
    }
  }

  //
  // KeyedCaseClass methods
  //
  override def unapplied = Account.unapply(this)
}

class AccountStore @Inject() (schema: Schema) extends Saves[Account] with SavesCreatedUpdated[Account] {
  def authenticate(email: String, passwordAttempt: String): Either[AccountAuthenticationError, Account] = {
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
    val emailInLowerCase = email.trim().toLowerCase
    from(schema.accounts)(account =>
      where(account.email === emailInLowerCase)
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
      theOld.created := theNew.created,
      theOld.updated := theNew.updated,
      theOld.celebrityId := theNew.celebrityId,
      theOld.customerId := theNew.customerId,
      theOld.administratorId := theNew.administratorId
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

class AccountCredentialsError extends AccountAuthenticationError

class AccountPasswordNotSetError extends AccountAuthenticationError

class AccountNotFoundError extends AccountAuthenticationError
