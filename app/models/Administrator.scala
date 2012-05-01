package models

import java.sql.Timestamp
import services.db.{KeyedCaseClass, Saves, Schema}
import com.google.inject.Inject
import services.{Utils, AppConfig, Time}

/**
 * Persistent entity representing administrators of our service.
 */
case class Administrator(
  id: Long = 0L,
  role: Option[String] = Some(AdminRole.Superuser.value),
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  services: AdministratorServices = AppConfig.instance[AdministratorServices]
) extends KeyedCaseClass[Long] with HasCreatedUpdated {
  override def unapplied = Administrator.unapply(this)

  def save(): Administrator = {
    services.store.save(this)
  }
}

case class AdministratorServices @Inject()(store: AdministratorStore, accountStore: AccountStore)

class AdministratorStore @Inject()(schema: Schema, accountStore: AccountStore) extends Saves[Administrator] with SavesCreatedUpdated[Administrator] {
  import org.squeryl.PrimitiveTypeMode._

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
  // Saves[Administrator] methods
  //
  override val table = schema.administrators

  override def defineUpdate(theOld: Administrator, theNew: Administrator) = {
    updateIs(
      theOld.created := theNew.created,
      theOld.role := theNew.role,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Administrator] methods
  //
  override def withCreatedUpdated(toUpdate: Administrator, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}

abstract sealed class AdminRole(val value: String)

object AdminRole {
  case object Superuser extends AdminRole("Superuser")
  case object AdminDisabled extends AdminRole("AdminDisabled")

  private val states = Utils.toMap[String, AdminRole](Seq(
    Superuser,
    AdminDisabled
  ), key=(theState) => theState.value)

  def apply(value: String) = {
    states(value)
  }
}
