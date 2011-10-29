package models

import play.db.jpa.{QueryOn, JPABase, Model}
import play.data.validation.{Email, Required}
import javax.persistence.{Column, InheritanceType, Inheritance, Entity}
import libs.Utils.optional

/**
 * Base class for all types of individuals identified by our system.
 * Any specific type (e.g. Administrator, Customer) should extend this class.
 */
@Entity
@Inheritance(strategy=InheritanceType.JOINED)
abstract class User extends Model with CreatedUpdated with PasswordProtected
{
  /**
   * The user's email address, which we use to uniquely identify him/her.
   */
  @Required @Email @Column(unique=true, nullable=false)
  var email: String = ""
  
  /**
   * The user's real name.
   */
  def name: Option[String] = optional(_name)
  def name_= (newName: String) { _name = newName }
  private var _name: String = ""
  
}

/**
 * Trait that provides User subclass companion objects with useful,
 * type-safe, user-specific queries.
 */
trait UserQueryOn[T <: JPABase with User] { this: QueryOn[T] =>
  /**
   * Find an instance of your User subtype by e-mail address,
   * if the user exists in the system.
   */
  def findByEmail(email: String)
                 (implicit m: scala.reflect.Manifest[T]): Option[T] =
  {
    this.find("email=:email", Map("email" -> email)).first()
  }
}
