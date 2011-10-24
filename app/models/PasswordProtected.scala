package models

import play.data.validation.Required
import play.libs.Crypto
import play.db.jpa.{JPABase, QueryOn, Model}

trait PasswordProtected { this: Model =>
  @Required
  var email: String = null

  @Required
  var passwordHash: String = null

  def setPassword (newPassword: String): Unit = {
    passwordHash = Crypto.passwordHash(newPassword)
  }
}

trait PasswordQueryOn[T <: JPABase with PasswordProtected] { this: QueryOn[T] =>
  def findByEmailAndPassword(email: String, password: String)
                            (implicit m: scala.reflect.Manifest[T]): Option[T] = {
    val query = find(
      "email=:email and passwordHash=:passwordHash",
      Map("email" -> email, "passwordHash" -> Crypto.passwordHash(password))
    )

    query.first()
  }
}
