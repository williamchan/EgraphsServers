package services.social

import net.codingwell.scalaguice.ScalaModule
import com.google.inject.{Singleton, AbstractModule}

/**
 * Installs Guice bindings for our social media code
 */
object SocialModule extends AbstractModule with ScalaModule {
  def configure() {
    bind[String].annotatedWith[FacebookAppId].toProvider[FacebookAppIdProvider].in[Singleton]
  }
}
