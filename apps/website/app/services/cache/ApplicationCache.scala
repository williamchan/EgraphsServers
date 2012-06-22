package services.cache

import services.http.{PlayId, HostInfo}
import com.google.inject.Inject
import play.libs.Time
import services.logging.Logging

/**
 * Cache for the current application ID.
 *
 * Delegates to the default [[services.cache.Cache]] implementation, but namespaces all of the keys it
 * inserts. When running in test mode, the namespace tries its best to be specific to the
 * machine by concatenating a bunch of identifying information (e.g. local username, mac address).
 *
 * When running in any production mode (e.g. live) it namespaces to just the name of the mode,
 * for example "live".
 *
 * Usage:
 * {{{
 *   class MyClass @Inject() (cache: ApplicationCache) {
 *     // put sumpin in for 30 days and 1 hour, respectively. They will be deleted if not
 *     // touched for that amount of time. Defaults to thirty days.
 *     cache.put("a string", "herp", Some("30d"))
 *     cache.put("a number", "1h")
 *
 *     // take sumpin out. Can also use .get if you feel like it
 *     cache[String]("a string") == "herp" // True
 *     cache[Int]("a number") == 1 // True
 *     cache[Int]("a string") + 1 == 2 // Throws an exception
 *   }
 * }}}
 *
 * @param cacheFactory function that yields a Cache implementation
 * @param hostInfo information about the running host. Used to generate a key namespace for
 *   the application.
 * @param playId the current play ID (e.g. "test", "live")
 */
case class ApplicationCache @Inject()(
  cacheFactory: () => Cache,
  hostInfo: HostInfo,
  @PlayId playId: String
) {
  import ApplicationCache._

  /**
   * Looks up the target key in the cache, returning an option of its type. See class
   * documentation for usage info
   *
   * Same as `get`.
   *
   * @param key key to look up
   * @tparam T type the type to look up. This is not type-safe, so make sure you know
   *   what type you're looking for.
   * @return Some(whatYouWanted) if it was found, otherwise None
   */
  def apply[T : Manifest](key: String): Option[T] = {
    val result = cache.get[T](fullKey(key))
    log("GET " + key + " -> " + result)

    result
  }

  /** See apply */
  def get[T : Manifest](key: String): Option[T] = {
    this.apply[T](key)
  }

  /**
   * Sets a cache value for a particular amount of time.
   *
   * See cache.apply
   *
   * @param key the key to look up
   * @param value
   * @param expiration the expiration date. See [[play.libs.Time.parseDuration()]]
   *     for syntax.
   */
  def set[T](key: String, value: T, expiration: Option[String]=None) {
    cache.set(fullKey(key), value, parseDuration(expiration))
    log("SET " + key + " -> " + value)
  }

  /**
   * Deletes the key from the cache if it was there to begin with.
   */
  def delete(key: String) {
    cache.delete(fullKey(key))
  }

  //
  // Private members
  //
  private[cache] def parseDuration(expirationOption: Option[String]): Int = {
    // Make it last for a month if not provided
    expirationOption.map(expiration => Time.parseDuration(expiration)).getOrElse(60 * 60 * 24 * 30)
  }

  private[cache] def fullKey(key: String): String = {
    hostId + "/" + key
  }

  private[cache] def fullKeyBytes(key: String): Array[Byte] = {
    fullKey(key).getBytes
  }

  private[cache] val hostId: String = {
    playId match {
      // For test instances we don't want to collide namespaces with production ones
      // or with other test ones
      case "test" =>
        "test_" + hostInfo.userName + "_" + hostInfo.computerName + "_" + hostInfo.macAddress

      // All production instances with a given id should share cache namespace
      case _ =>
        playId
    }
  }

  private def cache: Cache = {
    cacheFactory()
  }
}

object ApplicationCache extends Logging