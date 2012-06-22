package services.cache

import services.http.{PlayId, HostInfo}
import com.google.inject.Inject
import play.libs.Time

case class ApplicationCache @Inject()(
  cacheFactory: () => Cache,
  hostInfo: HostInfo,
  @PlayId playId: String
) {
  def set[T <: AnyRef](key: String, value: T, expiration: Option[String]=None) {
    cache.set(fullKey(key), value, parseDuration(expiration))
  }

  def get[T <: AnyRef](key: String): Option[T] = {
    cache.get(fullKey(key))
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
