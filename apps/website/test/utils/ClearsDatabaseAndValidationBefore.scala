package utils

import org.squeryl.PrimitiveTypeMode._
import org.scalatest.{Suite, BeforeAndAfterEach}
import play.data.validation.Validation
import services.blobs.Blobs
import services.{Time, AppConfig}
import services.db.{TransactionSerializable, DBSession, Schema}
import services.cache.CacheFactory
import junitx.util.PrivateAccessor
import collection.mutable.ArrayBuffer
import org.squeryl.{Session, Table}

/**
 * Mix this trait in to your Suite class to make sure that Play Validation
 * and all DB data are scrubbed in between test runs.
 */
trait ClearsDatabaseAndValidationBefore extends BeforeAndAfterEach { this: Suite =>
  override def beforeEach() {
    super.beforeEach()
    Validation.clear()
    AppConfig.instance[Blobs].scrub()
    AppConfig.instance[CacheFactory].applicationCache.clear()
    val (_, timeInSecond) = Time.stopwatch {
      AppConfig.instance[DBSession].connected(TransactionSerializable) {
        truncateTables
//        AppConfig.instance[Schema].scrub()
      }
    }

    println("Dropping the db took: " + timeInSecond + "s")
  }

  private def truncateTables {
    val schema = AppConfig.instance[Schema]
    val tables = PrivateAccessor.getField(schema, "org$squeryl$Schema$$_tables").asInstanceOf[ArrayBuffer[Table[_]]]
//    tables.foreach(table => table.deleteWhere(_ => 1===1))
    val tableNames = tables.map(table => table.name)
    val statement = Session.currentSession.connection.prepareStatement("TRUNCATE " + tableNames.mkString(", "))
    println("TRUNCATE " + tableNames.mkString(", "))
    statement.execute()
    statement.close()
  }

}
