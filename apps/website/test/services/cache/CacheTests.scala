package services.cache

import utils.{TestData, TestAppConfig, EgraphsUnitTest}
import uk.me.lings.scalaguice.ScalaModule
import com.google.inject.AbstractModule
import services.http.PlayConfig
import services.AppConfig
import java.util

trait CacheTests { this: EgraphsUnitTest =>
  def cacheInstance: Cache

  import TestData.makeTestCacheKey

  "A cache" should "return None for an empty key" in {
    cacheInstance.get[String](makeTestCacheKey) should be (None)
  }

  "A cache" should "set and get a variety of values" in {
    def values = List(
      "herp",
      Map("1" -> "2"),
      List("1", "2", "3"),
      5,
      2.22
    )

    for (value <- values) {
      deletingKey(makeTestCacheKey) { (cache, key) =>
        cache.set(key, value, 5)
        cache.get(key) should be (Some(value))
      }
    }
  }

  "A cache" should "set and get a Map" in {
    deletingKey(makeTestCacheKey) { (cache, key) =>
      val value = Map(
        "red" -> "blue",
        "one" -> "two",
        "herp" -> "derp",
        "alpha" -> "omega",
        "hard" -> "soft"
      )
      cache.set(key, value, 5)
      cache.get[scala.collection.Map[String,String]](key) should be (Some(value))
    }
  }

  "A cache" should "delete a key" in {
    deletingKey(makeTestCacheKey) { (cache, key) =>
      val value = "herp"
      cache.set(key, value, 5)
      cache.delete(key)
      cache.get[String](key) should be (None)
    }
  }

  "A cache" should "respect the expiration deadline" in {
    deletingKey(makeTestCacheKey) { (cache, key) =>
      val value = "herp"
      cache.set(key, value, 1)
      Thread.sleep(2000)
      cache.get[String](key) should be (None)
    }
  }

  private def deletingKey(key: String)(operation: (Cache, String) => Any) = {
    val cache = cacheInstance
    try {
      operation(cache, key)
    } finally {
      cache.delete(key)
    }
  }
}

class InMemoryCacheTests extends EgraphsUnitTest with CacheTests {
  def cacheInstance: Cache = {
    AppConfig.instance[CacheFactory].inMemoryCache
  }
}


class RedisCacheTests extends EgraphsUnitTest {
  def cacheInstance: Cache = {
    AppConfig.instance[CacheFactory].redisCacheOrBust()
  }

  // This test looks weird, but basically our one request gets one Jedis instance
  // from the redis plug-in. A Jedis is the low-level redis connection that handles pooling
  // for us. The result is that every time we get a new cacheFromAppCacheWithCacheSetting
  // it's just setting the same Jedis connection's db number again. So in order to test whether
  // our code actually selects the real one, we have to keep grabbing new ones from AppConfig
  // even if only to discard them.
  "Namespaced redis caches" should "actually be namespaced" in {
    import CacheFactoryTests.cacheFromAppWithCacheSetting

    // Set up
    val (key, db1Value, db2Value) = (TestData.makeTestCacheKey, "herp", "derp")
    def firstDb = cacheFromAppWithCacheSetting("redis.12")
    def secondDb = cacheFromAppWithCacheSetting("redis.13")

    // Run test
    firstDb.set(key, db1Value, 5)
    secondDb.set(key, db2Value, 5)

    // Check expectations
    firstDb.get(key) should be (Some(db1Value))
    secondDb.get(key) should be (Some(db2Value))
  }
}


class CacheFactoryTests extends EgraphsUnitTest {
  import CacheFactoryTests._

  "The bound cache factory" should "return the correct values for each server environment" in {
    AppConfig.instance[() => Cache].apply().getClass should be (classOf[InMemoryCache])
    cacheFromAppWithCacheSetting("memory").getClass should be(classOf[InMemoryCache])
    cacheFromAppWithCacheSetting("redis").getClass should be (classOf[RedisCache])
    cacheFromAppWithCacheSetting("redis.12").getClass should be (classOf[RedisCache])
    cacheFromAppWithCacheSetting("redis.13").getClass should be (classOf[RedisCache])
  }
}


object CacheFactoryTests {
  private[cache] def cacheFromAppWithCacheSetting(cacheConfig: String): Cache = {
    val factory = appConfigWithCacheSetting(cacheConfig).instance[() => Cache]
    factory()
  }

  private def appConfigWithCacheSetting(cacheConfig: String): TestAppConfig = {
    new TestAppConfig(new AbstractModule with ScalaModule {
      def configure() {
        val props = new util.Properties()
        props.put("application.cache", cacheConfig)
        bind[util.Properties].annotatedWith[PlayConfig].toInstance(props)
      }
    })
  }

}