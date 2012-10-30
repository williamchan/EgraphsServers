package services.cache

import uk.me.lings.scalaguice.ScalaModule
import com.google.inject.AbstractModule
import com.google.inject.Singleton

/**
 * Guice bindings that provide cache-related services.
 */
object CacheModule extends AbstractModule with ScalaModule {
  def configure() {
    bind[CacheFactory].in[Singleton]
  }
}
