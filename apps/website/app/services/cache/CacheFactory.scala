package services.cache

import services.Utils
import com.google.inject.Inject
import services.logging.Logging
import services.http.{PlayId, DeploymentTarget, HostInfo}
import play.api.cache.EhCachePlugin
import services.config.ConfigFileProxy

/**
 * Yields the configured [[services.cache.Cache]] implementation. See the documentation for
 * that class for information on how to use our cacheing layer.
 *
 * Implementation selection is governed by the play configuration "application.cache"
 * property. Valid values:
 *
 * - "redis": A Redis cache implementation against our hosted distributed cache. If the connection
 *       can't be made to the cache server then it defaults to the in-memory cache. It defaults to
 *       the fifth database index in redis.
 *
 * - "redis.1", "redis.2", etc: A Redis cache implementation against a specific database index 0 - 15
 *       on our hosted distributed cache.
 *
 * - "memory": An in-memory implementation that delegates to Play's in-memory cache
 **/
class CacheFactory @Inject()(
  config: ConfigFileProxy,
  @PlayId playId: String,
  hostInfo: HostInfo
)
{
  import CacheFactory._

  def applicationCache: NamespacedCache = {
    namespacedCache(hostId)
  }

  //
  // Private members
  //
  private[cache] val hostId: String = {
    if (playId == DeploymentTarget.Test) {
      // For test instances we don't want to collide namespaces with production ones
      // or with other test ones
      "test_" + hostInfo.userName + "_" + hostInfo.computerName + "_" + hostInfo.macAddress
    } else {
      // All production instances with a given id should share a cache namespace.
      // This is redundant with the fact that they get their own DB indeces on redis
      // (see redis documentation in application.conf
      playId
    }
  }

  private[cache] def namespacedCache(namespace: String = ""): NamespacedCache = {
    new NamespacedCache(namespace=namespace, cache=lowLevelCache)
  }

  private[cache] def lowLevelCache: Cache = {
    val appCacheValue = config.applicationCache

    appCacheValue.split("\\.").toList match {
      case List("memory") =>
        inMemoryCache

      case List("redis", dbNumber) =>
        redisCache(db = dbNumber.toInt)

      case List("redis") =>
        redisCache()

      case unrecognized =>
        throw new IllegalArgumentException(
          "Unrecognized play configuration value for \"application.cache:\" " + unrecognized
        )
    }
  }

  //
  // Private members
  //
  private[cache] def inMemoryCache: Cache = {
    new InMemoryCache()
  }

  private[cache] def redisCache(db: Int = JedisFactory.defaultRedisDb): Cache = {
    new RedisCache(new JedisFactory(db), config)
  }
}

object CacheFactory extends Logging
