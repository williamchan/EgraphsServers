package services.cache

import services.http.{PlayId, HostInfo}
import java.io._
import com.google.inject.Inject
import play.libs.Time

class ApplicationCache @Inject()(
  cacheFactory: () => Cache,
  hostInfo: HostInfo,
  @PlayId playId: String
) {
  def set[T <: Serializable](key: String, value: T, expiration: Option[String]=None) {
    cache.set(fullKey(key), value, parseDuration(expiration))
  }

  def get[T <: Serializable : Manifest](key: String): Option[T] = {
    cache.get(fullKey(key))
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

  private[cache] def hostId: String = {
    playId match {
      // For test instances we don't want to collide namespaces with production ones
      // or with other test ones
      case "test" =>
        hostInfo.userName + "@" + hostInfo.computerName + "(" + hostInfo.macAddress + ")"

      // All production instances with a given id should share cache namespace
      case _ =>
        playId
    }
  }

  private def cache: Cache = {
    cacheFactory()
  }
}
