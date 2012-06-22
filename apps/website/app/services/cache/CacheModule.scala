package services.cache

import uk.me.lings.scalaguice.ScalaModule
import com.google.inject.AbstractModule

/**
 * Guice bindings that provide cache-related services.
 */
object CacheModule extends AbstractModule with ScalaModule {
  def configure() {
    bind[() => Cache].to[CacheFactory]
  }
}
