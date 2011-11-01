package db

import org.squeryl.adapters.{PostgreSqlAdapter, MySQLAdapter, H2Adapter}
import org.squeryl.internals.DatabaseAdapter
import play.Play

/**
 * Provides various Squeryl database adapters based on what type of database we're
 * running against.
 */
object Adapter {
  lazy val h2 = new H2Adapter
  lazy val mysql = new MySQLAdapter
  lazy val postgres = new PostgreSqlAdapter

  def currentDbString = {
    Play.configuration.getProperty("db")
  }

  /**
   * Returns a Squeryl DatabaseAdapter given current Play! database string
   */
  def current: DatabaseAdapter = {
    getForDbString(Play.configuration.getProperty("db"))
  }

  def getForDbString(dbString: String): DatabaseAdapter = {
    def itContains(test: String): Boolean = dbString.contains(test)

    dbString match {
      case _ if itContains("mem") => h2
      case _ if itContains("mysql") => mysql
      case _ if itContains("postgres") => postgres
      case _ => throw new IllegalArgumentException(dbString)
    }
  }
}
