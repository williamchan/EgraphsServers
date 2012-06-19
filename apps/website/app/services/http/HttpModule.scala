package services.http

import uk.me.lings.scalaguice.ScalaModule
import com.google.inject.{AbstractModule}
import play.Play
import java.util.Properties

/**
 * Installs Guice application bindings that relate to our http services
 */
object HttpModule extends AbstractModule with ScalaModule {
  override def configure() {
    bind[Properties].annotatedWith[PlayConfig].toInstance(Play.configuration)
    bind[String].annotatedWith[PlayId].toInstance(Play.id)

    bind[RequireAuthenticityTokenFilter].toProvider[RequireAuthenticityTokenFilterProvider]
  }
}