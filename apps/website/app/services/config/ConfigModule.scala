package services.config

import com.google.inject.{Singleton, AbstractModule, Provider}

import net.codingwell.scalaguice.ScalaModule
import play.api.Play

/**
 * Installs Guice application bindings that relate to our http services
 */
object ConfigModule extends AbstractModule with ScalaModule {
  override def configure() {
    bind[ConfigFileProxy].toProvider[ConfigFileProxyProvider].in[Singleton]
  }
}

private[config] class ConfigFileProxyProvider extends Provider[ConfigFileProxy] {
  override def get(): ConfigFileProxy = {
    new ConfigFileProxy(Play.current.configuration)
  }
}