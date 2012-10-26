package services.cache

import services.logging.Logging
import services.Namespacing

/**
 * Cache implementation that can change its namespace. Good for namespaceing keys.
 *
 * WARNING: CLEARing this cache will clear THE ENTIRE CACHE, not just the namespace!
 *
 * For example:
 * {{{
 *   val baseNamespace = getANamespacedCache() // CacheFactory can probably give you one
 *   val ns1 = baseNamespace.namespaced(baseNamespace)
 *
 *   // SETS "herp" -> "derp"
 *   baseNamespace.set("herp", "derp")
 *
 *   // SETS "ns1/herp" -> "derp"
 *   ns1.set("herp", "ns1-derp")
 *
 *   baseNamespace.get("herp") == "derp" // True
 *   ns1.get("herp") == "ns1-derp" // True
 * }}}
 *
 * @param namespace the namespace
 * @param cache the cache implementation to which we delegate after namespacing the keys.
 */
class NamespacedCache (val namespace: String="", cache: Cache) extends Cache with Namespacing {
  import NamespacedCache._

  def namespaced(newNamespace: String): NamespacedCache = {
    new NamespacedCache(applyNamespace(newNamespace), cache)
  }

  //
  // Cache members
  //
  def set[T](key: String, value: T, expirationSeconds: Int) {
//    log("SET " + key + " -> " + value)
    cache.set(applyNamespace(key), value, expirationSeconds)
  }

  def get[T: Manifest](key: String): Option[T] = {
    val value = try {
      cache.get[T](applyNamespace(key))
      //    log("GET " + key + " -> " + value)
    } catch {
      case e: Exception =>
        error("Exception thrown from ImageAsset.url on key " + key)
        throw e
    }
    value
  }

  def delete(key: String) {
    cache.delete(applyNamespace(key))
  }

  def clear() {
    // TODO make this only clear keys within the namespace
    cache.clear()
  }
}


object NamespacedCache extends Logging