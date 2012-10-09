package services.http

import com.google.inject.AbstractModule

import play.api.Play
import services.http.filters.RequireAuthenticityTokenFilter
import services.http.filters.RequireAuthenticityTokenFilterProvider
import uk.me.lings.scalaguice.ScalaModule

/**
 * Installs Guice application bindings that relate to our http services
 */
object HttpModule extends AbstractModule with ScalaModule {
  override def configure() {    
    bind[String].annotatedWith[PlayId].toInstance(
      Play.current.configuration.getString("application.id").getOrElse("test")
    )
    bind[RequireAuthenticityTokenFilter].toProvider[RequireAuthenticityTokenFilterProvider]
  }
}
