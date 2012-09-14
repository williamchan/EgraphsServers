package services.cache

import play.cache.EhCacheImpl
import com.google.inject.Inject
import services.logging.Logging

/**
 * Default in-memory cache implementation. Prefer getting a Cache
 * instance by injecting a CacheFactory into your class and grabbing
 * its applicationCache property.
 *
 * @param cache the EhCacheImpl from play.
 */
private[cache] class InMemoryCache @Inject()(cache: EhCacheImpl) extends Cache {
  //
  // Cache members
  //
  override def set[T](key: String, value: T, expirationSeconds: Int) {
    cache.set(key, value.asInstanceOf[AnyRef], expirationSeconds)
  }

  override def get[T: Manifest](key: String): Option[T] = {
    // No, this is not type-safe. There's no way to make it so.
    Option(cache.get(key)).map(value => value.asInstanceOf[T])
  }

  override def clear() {
    cache.clear()
  }

  override def delete(key: String) {
    cache.delete(key)
  }
}

private[cache] object InMemoryCache extends Logging
