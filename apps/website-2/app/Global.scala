import org.squeryl.{Session, SessionFactory}
import io.Source
import java.io.{File, PrintWriter}
import play.api.Application
import play.api.Play
import play.api.GlobalSettings
import services.blobs.Blobs
import services.payment.Payment
import java.sql.Connection
import services.logging.{Logging, LoggingContext}
import services.config.ConfigFileProxy
import services.{AppConfig, Utils, TempFile}
import services.db.{TransactionSerializable, Schema, DBSession}
import models.{AccountStore, Account, Administrator}
import services.mvc.celebrity.{CatalogStarsActor, UpdateCatalogStarsActor}
import services.http.PlayId


object Global extends GlobalSettings with Logging {
  override def onStart(app: Application) {
    val logging = AppConfig.instance[LoggingContext]
    logging.withTraceableContext("Bootstrap") {
      val configProxy = AppConfig.instance[ConfigFileProxy]
      val blobs = AppConfig.instance[Blobs]
      val payment = AppConfig.instance[Payment]

      log("Configuration is: " + configProxy.applicationId)
      log("Bootstrapping application")

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

      // Initialize S3 or fs-based blobstore and cache
      blobs.init()
      services.cache.JedisFactory.startup()

      // Some additional test-mode setup
      if (configProxy.applicationId == "test") {
        TestModeBootstrap.run()
      }

      log("Finished bootstrapping application")
    }
  }
  
  override def onStop(app: Application) {
    services.cache.JedisFactory.shutDown()
  }
}

/**
 * Bootstrap code for when we're running in test mode
 */
private object TestModeBootstrap extends Logging {
  private val db = AppConfig.instance[DBSession]
  private val blobs = AppConfig.instance[Blobs]
  private val schema = AppConfig.instance[Schema]
  private val accountStore = AppConfig.instance[AccountStore]

  def run() {
    log("Performing test-mode bootstrap.")
    bootstrapDatabase()
    createTestAdmin()
    log("Finished bootstrapping test-mode.")
  }

  private def bootstrapDatabase() {
    db.connected(TransactionSerializable) {
      if ((!schema.isInPlace) || schemaHasChanged) {
        log(
          """Detected either lack of database schema or change thereof.
          (You can view the current schema at """ + schemaFile.getAbsolutePath + ")"
        )
        createNewSchema()
        blobs.scrub()
      }
    }
  }

  private def createTestAdmin() {
    val adminEmail = "admin@egraphs.com"
    db.connected(TransactionSerializable) {
      if (accountStore.findByEmail(adminEmail).isEmpty) {
        val administrator = Administrator().save()
        Account(email = adminEmail, administratorId = Some(administrator.id)).withPassword("egraphsa").right.get.save()
        log("Created Administrator with credentials admin@egraphs.com/derp")
      }
    }
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
