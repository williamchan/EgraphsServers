package models

import org.squeryl.PrimitiveTypeMode._
import play.data.validation.Validation.ValidationResult
import play.data.validation.Validation
import java.sql.Timestamp
import libs.Time
import db.{KeyedCaseClass, Saves, Schema}

/**
 * Basic account information for any user in the system
 */
case class Account(
  id: Long = 0,
  email: String = "",
  passwordHash: Option[String] = None,
  passwordSalt: Option[String] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  override def unapplied = Account.unapply(this)
  
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
        Right(copy(passwordHash=Some(password.hash), passwordSalt=Some(password.salt)))
    }
  }
}

object Account extends Saves[Account] with SavesCreatedUpdated[Account] {

  //
  // Saves[Account] methods
  //
  override val table = Schema.accounts

  override def defineUpdate(theOld: Account, theNew: Account) = {
    updateIs(
      theOld.email := theNew.email,
      theOld.passwordHash := theNew.passwordHash,
      theOld.passwordSalt := theNew.passwordSalt,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }
  
  //
  // SavesCreatedUpdated[Account] methods
  //
  override protected def withCreatedUpdated(toUpdate: Account,
                                            created: Timestamp,
                                            updated: Timestamp): Account =
  {
    toUpdate.copy(created=created, updated=updated)
  }
}