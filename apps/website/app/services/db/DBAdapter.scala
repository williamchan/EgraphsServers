package services.db

import org.squeryl.internals.DatabaseAdapter
import play.api.Play
import org.squeryl.adapters.{MySQLInnoDBAdapter, PostgreSqlAdapter, H2Adapter}
import services.logging.Logging
import services.AppConfig
import services.config.ConfigFileProxy

/**
 * Provides various Squeryl database adapters based on what type of database we're
 * running against.
 */
object DBAdapter extends Logging {  
  lazy val h2 = new H2Adapter
  lazy val mysql = new MySQLInnoDBAdapter
  lazy val postgres = new PostgreSqlAdapter {override def quoteIdentifier(s: String) = s}

  /**
   * Returns a Squeryl DatabaseAdapter given current Play! database string
   */
  def current: DatabaseAdapter = {
    getForDbString(config.dbDefaultUrl)
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
  
  //
  // Private members
  //
  val config = AppConfig.instance[ConfigFileProxy]
}
