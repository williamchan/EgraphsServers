package services.http

import com.google.inject.AbstractModule

import play.api.Play
import services.inject.ClosureProviders
import services.http.filters.RequireAuthenticityTokenFilter
import services.http.filters.RequireAuthenticityTokenFilterProvider
import net.codingwell.scalaguice.ScalaModule

/**
 * Installs Guice application bindings that relate to our http services
 */
object HttpModule extends AbstractModule with ScalaModule with ClosureProviders {
  override def configure() {    
    bind[String].annotatedWith[PlayId].toProvider {
      Play.current.configuration.getString("application.id").getOrElse("test")
    }
    bind[RequireAuthenticityTokenFilter].toProvider[RequireAuthenticityTokenFilterProvider]
  }
}
