package services.cache

import services.http.{PlayId, HostInfo}
import com.google.inject.Inject
import play.libs.Time
import services.logging.Logging

case class ApplicationCache @Inject()(
  cacheFactory: () => Cache,
  hostInfo: HostInfo,
  @PlayId playId: String
) {
  import ApplicationCache._

  def apply[T : Manifest](key: String): Option[T] = {
    val result = cache.get[T](fullKey(key))
    log("GET " + key + " -> " + result)

    result
  }

  def get[T : Manifest](key: String): Option[T] = {
    this.apply[T](key)
  }

  def set[T](key: String, value: T, expiration: Option[String]=None) {
    cache.set(fullKey(key), value, parseDuration(expiration))
    log("SET " + key + " -> " + value)
  }

  def delete(key: String) = {
    cache.delete(fullKey(key))
  }

  // Protected helper methods
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