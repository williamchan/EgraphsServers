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
    cacheInstance.get(makeTestCacheKey) should be (None)
  }

  "A cache" should "set and get a String" in {
    deletingKey(makeTestCacheKey) { (cache, key) =>
      cache.set(key, "herp", 5)
      cache.get(key) should be (Some("herp"))
    }
  }

  "A cache" should "set and get a Map" in {
    deletingKey(makeTestCacheKey) { (cache, key) =>
      val value = scala.collection.immutable.Map("red" -> "blue")
      cache.set(key, value, 5)
      cache.get[Map[String, String]](key) should be (Some(value))
    }
  }

  "A cache" should "delete a key" in {
    deletingKey(makeTestCacheKey) { (cache, key) =>
      val value = "herp"
      cache.set(key, value, 5)
      cache.delete(key)
      cache.get(key) should be (None)
    }
  }

    "A cache" should "respect the expiration deadline" in {
      deletingKey(makeTestCacheKey) { (cache, key) =>
        val value = "herp"
        cache.set(key, value, 1)
        Thread.sleep(2000)
        cache.get(key) should be (None)
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


class RedisCacheTests extends EgraphsUnitTest with CacheTests {
  def cacheInstance: Cache = {
    AppConfig.instance[CacheFactory].redisCache
  }
}


class CacheFactoryTests extends EgraphsUnitTest {
  "The bound cache factory" should "return the correct values for each server environment" in {
    AppConfig.instance[() => Cache].apply().getClass should be (classOf[InMemoryCache])
    cacheFromAppWithCacheSetting("memory").getClass should be(classOf[InMemoryCache])
    cacheFromAppWithCacheSetting("redis").getClass should be (classOf[RedisCache])
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

  private def cacheFromAppWithCacheSetting(cacheConfig: String): Cache = {
    val factory = appConfigWithCacheSetting(cacheConfig).instance[() => Cache]
    factory()
  }

  private def cacheFactory = {
    AppConfig.instance[CacheFactory]
  }
}
