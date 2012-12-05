import java.io.{File, PrintWriter}
import java.sql.Connection
import org.squeryl.{Session, SessionFactory}
import models.{Account, AccountStore, Administrator}
import play.api.{Application, Play}
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}
import play.api.Play.current
import scala.io.Source
import services.{AppConfig, TempFile, Time, Utils}
import services.blobs.Blobs
import services.config.ConfigFileProxy
import services.db.{DBSession, Schema, TransactionSerializable}
import services.http.SSLConfig
import services.logging.{Logging, LoggingContext}
import services.mvc.celebrity.{CatalogStarsAgent, UpdateCatalogStarsActor}
import services.payment.Payment
import services.mvc.search.RebuildSearchIndexActor

object Global extends controllers.ToyBox with Logging {
  // for ToyBox; see .conf file in use for further configuration
  val loginPath = "/toybox/login"
  val assetsRoute = controllers.routes.EgraphsAssets.at(_)


  override def onStart(app: Application) {
    SSLConfig.enableCustomTrustore()
    
    val logging = AppConfig.instance[LoggingContext]
    logging.bootstrap()
    logging.withTraceableContext("Bootstrap") {
      val (_, secondsToBootstrap) = Time.stopwatch {
        val configProxy = AppConfig.instance[ConfigFileProxy]
        val blobs = AppConfig.instance[Blobs]
        val payment = AppConfig.instance[Payment]

        log("Configuration is: " + configProxy.applicationId)
        log("Bootstrapping application")

        // Initialize payment system
        payment.bootstrap()

        // Initialize Squeryl persistence
        SessionFactory.concreteFactory = Some(() => {
          val connection = play.api.db.DB.getConnection()(app)
          if (connection.getTransactionIsolation != Connection.TRANSACTION_SERIALIZABLE) {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE)
          }
          Session.create(connection, services.db.DBAdapter.current)
        })

        // Initialize S3 or fs-based blobstore and cache
        blobs.init()
        services.cache.JedisFactory.startup()

        // Some additional test-mode setup
        if (configProxy.applicationId == "test" &&
            configProxy.applicationMode == "dev" &&
            configProxy.dbDefaultUrl == "jdbc:postgresql://localhost/egraphs" &&
            configProxy.blobstoreVendor == "filesystem") {
          TestModeBootstrap.run()
        }

        // Schedule catalog stars updating
        UpdateCatalogStarsActor.init()

        // Schedule search index rebuilding
        if (configProxy.adminToolsEnabled == "full") {
          RebuildSearchIndexActor.init()
        }
      }
      log("Finished bootstrapping application in " + secondsToBootstrap + "s")
    }
  }
  
  override def onStop(app: Application) {
    services.cache.JedisFactory.shutDown()
    SSLConfig.disableCustomTrustore()
    CatalogStarsAgent.singleton.close()
  }
  
  /**
   * Egraphs error page for 400
   */
  override def onBadRequest(request: RequestHeader, error: String): Result = {
    log("400 error: " + request + " from request " + error)
    BadRequest(views.html.frontend.errors.bad_request())
  }
  
  /**
   * Egraphs error page for 500
   */
  override def onError(request: RequestHeader, ex: Throwable): Result = {
    if (Play.isProd) {
      log("500 error: " + request + " from request " + ex.getStackTraceString)
      InternalServerError(views.html.frontend.errors.error())
    } else {
      super.onError(request, ex)
    }
  }
  
  /**
   * Egraphs error page for 404
   */
  override def onHandlerNotFound(request: RequestHeader): Result = {
    NotFound(views.html.frontend.errors.not_found())
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
//         blobs.scrub()
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
