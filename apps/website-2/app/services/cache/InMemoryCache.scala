package services.cache

import com.google.inject.Inject
import services.logging.Logging
import net.sf.ehcache.CacheManager
import net.sf.ehcache.Element;

/**
 * Default in-memory cache implementation. Prefer getting a Cache
 * instance by injecting a CacheFactory into your class and grabbing
 * its applicationCache property.
 *
 * @param cache the EhCacheImpl from play
 */
private[cache] class InMemoryCache @Inject() extends Cache {
  import InMemoryCache.cache

  //
  // Cache members
  //
  override def set[T](key: String, value: T, expirationSeconds: Int) {
    val element = new Element(key, value);
    element.setTimeToLive(expirationSeconds);
    cache.put(element);
  }

  override def get[T: Manifest](key: String): Option[T] = {
    // No, this is not type-safe. There's no way to make it so.
    Option(cache.get(key)).map(_.getObjectValue.asInstanceOf[T])
  }

  override def clear() {
    cache.removeAll()
  }

  override def delete(key: String) {
    cache.remove(key)
  }
}

private[cache] object InMemoryCache extends Logging {
  lazy val (manager, cache) = {
    val manager = CacheManager.create()
    if (!manager.cacheExists("play")) manager.addCache("play")
    (manager, manager.getCache("play"))
  }
}
