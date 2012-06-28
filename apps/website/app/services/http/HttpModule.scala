package services.http

import uk.me.lings.scalaguice.ScalaModule
import com.google.inject.{AbstractModule}
import play.Play
import java.util.Properties
import play.mvc.Scope.Session

/**
 * Installs Guice application bindings that relate to our http services
 */
object HttpModule extends AbstractModule with ScalaModule {
  override def configure() {
    bind[Properties].annotatedWith[PlayConfig].toInstance(Play.configuration)
    bind[() => Session].toInstance(() => Session.current())
    bind[() => ServerSession].to[ServerSessionFactory]
    bind[() => EgraphsSession].to[EgraphsSessionFactory]

    bind[String].annotatedWith[PlayId].toInstance(Play.id)
    bind[RequireAuthenticityTokenFilter].toProvider[RequireAuthenticityTokenFilterProvider]
  }
}