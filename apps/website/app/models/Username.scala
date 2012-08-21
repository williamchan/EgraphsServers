package models

import java.sql.Timestamp
import services.{AppConfig, Time}
import services.db.KeyedCaseClass
import org.squeryl.annotations.Column

/**
 * Created with IntelliJ IDEA.
 * User: myyk
 * Date: 8/15/12
 * Time: 6:49 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 * Persistent entity representing a customer's username.  This may not be their current user name, but one that they
 * once had at the very least.  There should be at most 1 Username per customerId that isPermenant.
 */
case class Username(
  @Column("username")
  id: String = "",
  customerId: Long = 0L,
  isPermanent: Boolean = false,
  isRemoved: Boolean = false, // a username may be removed by admins if the name is too offensive for example
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[String] with HasCreatedUpdated {

  def username = id

  // Public methods
  //
  def save(): Username = {
    RichUsernameHistory(this).save()
  }

  //
  // KeyedCaseClass[String] methods
  //
  override def unapplied = {
    Username.unapply(this)
  }
}

case class RichUsernameHistory(usernameHistory: Username) {
  implicit def usernameHistory2RichUsernameHistory(usernameHistory: Username): RichUsernameHistory = {
    RichUsernameHistory(usernameHistory)
  }

  val services = AppConfig.instance[CustomerServices]

  /** Retrieves the UsernameHistory's Customer from the database */
  def customer: Customer = {
    services.customerStore.findById(usernameHistory.customerId).get
  }

  /**
   * True if the underlying Username can be permanent without breaking the data model's constraints, false otherwise.
   */
  lazy val canBePermanent: Boolean = {
    val allUsernames = services.usernameHistoryStore.findAllByCustomerId(usernameHistory.customerId)
    val allOtherUsernames = allUsernames.filterNot(history => usernameHistory.username == history.username)
    val hasPermanent = allOtherUsernames.exists(username => username.isPermanent)
    !hasPermanent
  }

  def save(): Username = {
    if(usernameHistory.isPermanent && !canBePermanent) {
      throw new Exception("There can only be one permenant username for each customerId.  Cannot make this username permenant.")
    }

    services.usernameHistoryStore.save(usernameHistory)
  }
}