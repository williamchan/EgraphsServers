package services.mail

import com.google.inject.{Singleton, AbstractModule}
import net.codingwell.scalaguice.ScalaModule

/**
 * Guice bindings for our email-related services.
 */
object MailModule extends AbstractModule with ScalaModule {
  override def configure() {
    bind[TransactionalMail].toProvider[MailProvider].in[Singleton]
    bind[BulkMailList].toProvider[BulkMailListProvider].in[Singleton]
  }
}