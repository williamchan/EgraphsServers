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
 * Persistent entity representing history of a customer's username.
 */
case class UsernameHistory(
  @Column("username")
  id: String = "",
  customerId: Long = 0L,
  isPermanent: Boolean = false,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[String] with HasCreatedUpdated {

  def username = id

  // Public methods
  //
  def save(): UsernameHistory = {
    RichUsernameHistory(this).save()
  }

  //
  // KeyedCaseClass[String] methods
  //
  override def unapplied = {
    UsernameHistory.unapply(this)
  }
}

case class RichUsernameHistory(usernameHistory: UsernameHistory) {
  implicit def usernameHistory2RichUsernameHistory(usernameHistory: UsernameHistory): RichUsernameHistory = {
    RichUsernameHistory(usernameHistory)
  }

  val services = AppConfig.instance[CustomerServices]

  /** Retrieves the UsernameHistory's Customer from the database */
  def customer: Customer = {
    services.customerStore.findById(usernameHistory.customerId).get
  }

  def save(): UsernameHistory = {
    services.usernameHistoryStore.save(usernameHistory)
  }
}