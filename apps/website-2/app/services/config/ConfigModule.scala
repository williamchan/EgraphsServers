package services.config

import com.google.inject.AbstractModule

import play.api.Configuration
import play.api.Play
import services.http.filters.RequireAuthenticityTokenFilter
import services.http.filters.RequireAuthenticityTokenFilterProvider
import uk.me.lings.scalaguice.ScalaModule
import services.inject.ClosureProviders
import play.api.Play

/**
 * Installs Guice application bindings that relate to our http services
 */
object ConfigModule extends AbstractModule with ScalaModule with ClosureProviders {
  override def configure() {
    bind[ConfigFileProxy].toProvider {
      new ConfigFileProxy(Play.current.configuration)
    }
  }
}
