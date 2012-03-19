package bootstrap

import play.jobs._
import org.squeryl.{Session, SessionFactory}
import io.Source
import java.io.{File, PrintWriter}
import play.Play
import services.blobs.Blobs
import services.{AppConfig, Utils, TempFile}
import services.db.{Schema, DBSession}
import services.payment.Payment
import java.sql.Connection

@OnApplicationStart
class BootStrap extends Job {
  val blobs = AppConfig.instance[Blobs]
  val payment = AppConfig.instance[Payment]

  override def doJob() {
    // Initialize payment system
    payment.bootstrap()

    // Initialize Squeryl persistence
    SessionFactory.concreteFactory = Some(() => {
      val connection = play.db.DB.getConnection
      if (connection.getTransactionIsolation != Connection.TRANSACTION_SERIALIZABLE) {
        connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE)
      }
      Session.create(connection, services.db.DBAdapter.current)
    })

    // Initialize S3 or fs-based blobstore
    blobs.init()

    // Some additional test-mode setup
    if (Play.id == "test") {
      TestModeBootstrap.run()
    }
  }
}

/**
 * Bootstrap code for when we're running in test mode
 */
private object TestModeBootstrap {
  val blobs = AppConfig.instance[Blobs]
  val schema = AppConfig.instance[Schema]

  import org.squeryl.PrimitiveTypeMode._

  def run() {
    bootstrapDatabase()
  }

  private def bootstrapDatabase() {
    DBSession.init()
    inTransaction {
      printDdlToFile(schemaFile)
      if ((!schema.isInPlace) || schemaHasChanged) {
        play.Logger.info(
          """Detected either lack of database schema or change thereof.
          (You can view the current schema at """ + schemaFile.getAbsolutePath + ")"
        )
        createNewSchema()
        blobs.scrub()
      }
    }
    DBSession.commit()
  }


  /**
   * Drops and re-creates the database definition, logging the creation SQL
   * to schemaFile.
   */
  private def createNewSchema() {
    // drop and re-create the database definition, logging the
    // creation SQL to a temporary file.
    schema.scrub()

    printDdlToFile(schemaFile)
  }

  /**Prints the database schema to the provided file. */
  private def printDdlToFile(file: File) {
    file.delete()

    Utils.closing(new PrintWriter(file)) {
      writer =>
        schema.printDdl(writer)
    }
  }

  /**
   * Returns true that the schema has changed since the last time
   * the server was run.
   */
  private def schemaHasChanged: Boolean = {
    if (schemaFile.exists()) {
      val prevDdl = Source.fromFile(schemaFile).mkString

      prevDdl != schema.ddl
    }
    else {
      // No schema file existed, so the schema is different whatever it is.
      true
    }
  }

  /**Location for log of the database schema SQL */
  def schemaFile = TempFile.named("schema.sql")
}