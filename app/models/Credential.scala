package models

import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import play.data.validation.Validation.ValidationResult
import play.data.validation.Validation
import db.{Saves, DB}
import java.sql.Timestamp

/**
 * Authentication credentials for any user of the system.
 */
case class Credential(
  id: Long = 0,
  email: String = "",
  passwordHash: Option[String] = Some(""),
  passwordSalt: Option[String] = Some(""),
  created: Timestamp = new Timestamp(0L),
  updated: Timestamp = new Timestamp(0L)
) extends KeyedEntity[Long] with HasCreatedUpdated
{
  /** Returns the password, which may or may not have been set. */
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
  def withPassword(newPassword: String): Either[ValidationResult, Credential] = {
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

object Credential extends Saves[Credential] with SavesCreatedUpdated[Credential] {

  /** Queries a credential by its ID */
  def byId(id: Long): Option[Credential] = {
    inTransaction {
      DB.credentials.lookup(id)
    }
  }

  //
  // Saves[Credential] methods
  //
  override val table = DB.credentials

  override def defineUpdate(theOld: Credential, theNew: Credential) = {
    updateIs(
      theOld.email := theNew.email,
      theOld.passwordHash := theNew.passwordHash,
      theOld.passwordSalt := theNew.passwordSalt
    )
  }
  
  //
  // SavesCreatedUpdated[Credential] methods
  //
  override protected def withCreatedUpdated(toUpdate: Credential,
                                            created: Timestamp,
                                            updated: Timestamp): Credential =
  {
    toUpdate.copy(created=created, updated=updated)
  }
}