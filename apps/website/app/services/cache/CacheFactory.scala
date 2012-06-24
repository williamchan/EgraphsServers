package services.cache

import services.Utils
import com.google.inject.Inject
import services.logging.Logging
import play.cache.EhCacheImpl
import services.http.{PlayId, DeploymentTarget, HostInfo}

/**
 * Yields the configured [[services.cache.Cache]] implementation.
 *
 * Implementation selection is governed by the play configuration "application.cache"
 * property. Valid values:
 *
 * - "redis": A Redis cache implementation against our hosted distributed cache. If the connection
 *       can't be made to the cache server then it defaults to the in-memory cache. It defaults to
 *       the fifth database index in redis.
 *
 * - "redis.1, redis.2, etc": A Redis cache implementation against a specific database index 0 - 15
 *       on our hosted distributed cache.
 *
 * - "memory": An in-memory implementation that delegates to Play's [[play.cache.EhCacheImpl]]
 **/
class CacheFactory @Inject()(
  utils: Utils,
  @PlayId playId: String,
  hostInfo: HostInfo,
  jedisFactory: JedisFactory
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
    if (playId == DeploymentTarget.Test.name) {
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
    val appCacheValue = utils.requiredConfigurationProperty("application.cache")

    appCacheValue.split("\\.").toList match {
      case List("memory") =>
        inMemoryCache

      case List("redis", dbNumber) =>
        redisCacheIfPossible(db = dbNumber.toInt)

      case List("redis") =>
        redisCacheIfPossible()

      case unrecognized =>
        throw new IllegalArgumentException(
          "Unrecognized play configuration value for \"application.cache:\" " + unrecognized
        )
    }
  }


  private[cache] def inMemoryCache: Cache = {
    new InMemoryCache(EhCacheImpl.getInstance())
  }

  private[cache] def redisCacheIfPossible(db: Int = JedisFactory.defaultRedisDb): Cache = {
    val maybeJedis = jedisFactory(db = db)
    val maybeRedisCache = maybeJedis.map(jedis => new RedisCache(jedis))

    maybeRedisCache.getOrElse {
      log("Falling back to in-memory cache due to failure to acquire redis connection")

      inMemoryCache
    }
  }

  private[cache] def redisCacheOrBust(db: Int = JedisFactory.defaultRedisDb): Cache = {
    jedisFactory(db).map(jedis => new RedisCache(jedis)).getOrElse {
      throw new Exception(
        "All we wanted was a redis cache implementation," +
          " but now we want to watch the world burn."
      )
    }
  }
}

object CacheFactory extends Logging
