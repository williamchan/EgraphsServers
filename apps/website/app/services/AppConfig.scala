package services

import blobs.BlobModule
import cache.CacheModule
import db.DBModule
import graphics.GraphicsModule
import http.HttpModule
import config.ConfigModule
import mail.{BulkMailList, BulkMailListProvider, MailProvider, TransactionalMail}
import models._
import models.vbg._
import models.xyzmo._
import mvc.MvcModule
import payment.PaymentModule
import signature.SignatureBiometricsModule
import social.SocialModule
import voice.VoiceBiometricsModule
import uk.me.lings.scalaguice.{InjectorExtensions, ScalaModule}
import com.google.inject.{Injector, Singleton, Guice, AbstractModule}
import services.logging.Logging
import com.google.inject.Stage
import services.config.ConfigFileProxy

class AppConfig extends AbstractModule with ScalaModule {
  override def configure() {
    install(services.mail.MailModule)
    install(DBModule)
    install(CacheModule)
    install(HttpModule)
    install(BlobModule)
    install(PaymentModule)
    install(SignatureBiometricsModule)
    install(VoiceBiometricsModule)
    install(GraphicsModule)
    install(SocialModule)
    install(MvcModule)
    install(ConfigModule)

    install(models.ModelModule)
  }
}

/**
 * Accessor to the application's dependency injector.
 */
object AppConfig extends Logging {

  import InjectorExtensions._
  import uk.me.lings.scalaguice.typeLiteral
  import uk.me.lings.scalaguice.KeyExtensions._

  val injector: Injector = {
    // Put a try-catch on making injector and print the error because Guice
    // exceptions get turned into UnexpectedExceptions by Play, making them
    // almost impossible to catch.
    val (createdInjector, timing) = Time.stopwatch {
      try {
        import play.api.Play

        val stage = if (new ConfigFileProxy(Play.current.configuration).applicationMode == "dev") {
          Stage.DEVELOPMENT
        } else {
          Stage.PRODUCTION
        }
        
        Guice.createInjector(stage, new AppConfig)
      }
      catch {
        case e: Throwable =>
          e.printStackTrace()
          throw e
      }
    }

    createdInjector
  }

  def instance[T: Manifest] = {
    injector.instance[T]
  }

  /**
   * Gets a class instance bound to a particular annotation. This can be useful for getting, for example,
   * the particular String instance which is bound to FbAppId.
   *
   * @tparam A the annotation type (e.g. FbAppId)
   * @tparam T the instance type (e.g. String)
   *
   * @return the instance in question, if it had a matching binding. Otherwise throws
   *     an exception.
   */
  def annotatedInstance[A <: java.lang.annotation.Annotation: Manifest, T: Manifest]: T = {
    injector.getInstance(typeLiteral[T].annotatedWith[A])
  }
}
