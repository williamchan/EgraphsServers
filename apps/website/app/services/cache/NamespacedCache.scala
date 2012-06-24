package services.cache

import services.logging.Logging
import services.Namespacing

class NamespacedCache (val namespace: String="", cache: Cache) extends Cache with Namespacing {
  import NamespacedCache._

  def namespaced(newNamespace: String): NamespacedCache = {
    new NamespacedCache(applyNamespace(newNamespace), cache)
  }

  //
  // Cache members
  //
  def set[T](key: String, value: T, expirationSeconds: Int) {
    log("SET " + key + " -> " + value)
    cache.set(applyNamespace(key), value, expirationSeconds)
  }

  def get[T: Manifest](key: String): Option[T] = {
    val value = cache.get[T](applyNamespace(key))
    log("GET " + key + " -> " + value)

    value
  }

  def delete(key: String) {
    cache.delete(applyNamespace(key))
  }

  def clear() {
    cache.clear()
  }
}


object NamespacedCache extends Logging