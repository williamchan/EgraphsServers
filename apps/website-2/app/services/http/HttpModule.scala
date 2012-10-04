package services.http

import uk.me.lings.scalaguice.ScalaModule
import com.google.inject.{AbstractModule}
import play.api.Play
import java.util.Properties
import services.http.filters.{RequireAuthenticityTokenFilter, RequireAuthenticityTokenFilterProvider}

/**
 * Installs Guice application bindings that relate to our http services
 */
object HttpModule extends AbstractModule with ScalaModule {
  override def configure() {
    bind[Properties].annotatedWith[PlayConfig].toInstance(PlayConfigurationProperties.properties)
    // TODO: PLAY20 migration. You can no longer have session factories.
    // replace everywhere that uses a session factory and make them take the
    // session in their method signature. Same applies to flash =(
    bind[() => Session].toInstance(() => Session.current())    
    bind[() => EgraphsSession].to[EgraphsSessionFactory]

    bind[String].annotatedWith[PlayId].toInstance(
      Play.current.configuration.getString("id").getOrElse("test")
    )
    bind[RequireAuthenticityTokenFilter].toProvider[RequireAuthenticityTokenFilterProvider]
  }
}
