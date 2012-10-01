package services.db

import org.squeryl.internals.DatabaseAdapter
import play.Play
import org.squeryl.adapters.{MySQLInnoDBAdapter, PostgreSqlAdapter, H2Adapter}
import services.logging.Logging

/**
 * Provides various Squeryl database adapters based on what type of database we're
 * running against.
 */
object DBAdapter extends Logging {
  lazy val h2 = new H2Adapter
  lazy val mysql = new MySQLInnoDBAdapter
  lazy val postgres = new PostgreSqlAdapter {override def quoteIdentifier(s: String) = s}

  def currentDbString = {
    Play.application().configuration().getString("db")
  }

  /**
   * Returns a Squeryl DatabaseAdapter given current Play! database string
   */
  def current: DatabaseAdapter = {
    try {
      getForDbString(Play.application().configuration().getString("db.url"))
    }
    catch {
      case e: IllegalArgumentException =>
        log(
          "Found no property 'db.url' in application.conf while configuring Squeryl. Trying 'db'"
        )
        getForDbString(currentDbString)
    }
  }

  def getForDbString(dbString: String): DatabaseAdapter = {
    def itContains(test: String): Boolean = dbString.contains(test)

    dbString match {
      case _ if itContains("mem") || itContains("fs") => h2
      case _ if itContains("mysql") => mysql
      case _ if itContains("postgres") => postgres
      case _ => throw new IllegalArgumentException(dbString)
    }
  }
}
