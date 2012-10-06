package services.social

import uk.me.lings.scalaguice.ScalaModule
import com.google.inject.{Singleton, AbstractModule}
import play.api.Play.current

/**
 * Installs Guice bindings for our social media code
 */
object SocialModule extends AbstractModule with ScalaModule {
  def configure() {
    bind[String].annotatedWith[FacebookAppId].toInstance(current.configuration.getString("fb.appid").get)
  }
}
