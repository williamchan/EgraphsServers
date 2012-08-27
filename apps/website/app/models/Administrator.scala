package models

import enums.AdminRole.EnumVal
import enums.{HasAdminRole, AdminRole}
import java.sql.Timestamp
import services.db.{KeyedCaseClass, SavesWithLongKey, Schema}
import com.google.inject.Inject
import services.{AppConfig, Time}

/**
 * Persistent entity representing administrators of our service.
 */
case class Administrator(
  id: Long = 0L,
  _role: String = AdminRole.Superuser.name,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: AdministratorServices = AppConfig.instance[AdministratorServices]
) extends KeyedCaseClass[Long]
  with HasCreatedUpdated
  with HasAdminRole[Administrator] {

  override def unapplied = Administrator.unapply(this)


  override def withRole(status: EnumVal) = {
    this.copy(_role = status.name)
  }

  def save(): Administrator = {
    services.store.save(this)
  }
}

case class AdministratorServices @Inject()(store: AdministratorStore, accountStore: AccountStore)

class AdministratorStore @Inject()(schema: Schema, accountStore: AccountStore) extends SavesWithLongKey[Administrator] with SavesCreatedUpdated[Long,Administrator] {
  import org.squeryl.PrimitiveTypeMode._

  def isAdmin(adminId: Option[Long]): Boolean = {
    adminId match {
      case None => false
      case Some(id) => findById(id).isDefined
    }
  }

  def authenticate(email: String, passwordAttempt: String): Option[Administrator] = {
    val authenticationResult: Either[AccountAuthenticationError, Account] = accountStore.authenticate(email = email, passwordAttempt = passwordAttempt)

    val administrator = if (authenticationResult.isRight) {
      val account = authenticationResult.right.get
      if (account.administratorId.isDefined) findById(account.administratorId.get) else None
    } else {
      None
    }
    administrator
  }

  def findByEmail(email: String): Option[Administrator] = {
    val accountOption = accountStore.findByEmail(email)
    val adminOption = accountOption.flatMap {
      account =>
        account.administratorId.flatMap {
          administratorId =>
            findById(administratorId)
        }
    }
    adminOption
  }

  //
  // SavesWithLongKey[Administrator] methods
  //
  override val table = schema.administrators

  override def defineUpdate(theOld: Administrator, theNew: Administrator) = {
    updateIs(
      theOld.created := theNew.created,
      theOld._role := theNew._role,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Long,Administrator] methods
  //
  override def withCreatedUpdated(toUpdate: Administrator, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}
