package services.http

import utils.EgraphsUnitTest
import services.AppConfig

class SessionCacheTests extends EgraphsUnitTest {
  "The session cache" should "start out empty" in {
    newCache.isEmpty should be (true)
  }

  "Saving a populated cache" should "work" in {
    deletingSessionCacheAfter {
      // Set up
      val (key, value) = testStringKeyValue
      val cache = newCache

      // Run test
      val valueBeforeSave = cache(key)
      cache.setting(key -> value).save()
      val valueAfterSave = newCache(key)

      // Check expectations
      valueBeforeSave should be (None)
      valueAfterSave should be (Some(value))
    }
  }

  "Saving an emptied cache" should "actually delete keys" in {
    deletingSessionCacheAfter {
      // Set up
      val (key, value) = testStringKeyValue
      val cache = newCache
      val savedCache = cache.setting(key -> value).save()

      // Run test
      val restoredBeforeEmpty = newCache(key)
      savedCache.emptied.save()
      val restoredAfterEmpty = newCache(key)

      // Check expectations
      restoredBeforeEmpty should be (Some(value))
      restoredAfterEmpty should be (None)
    }
  }

  "The session cache" should "not save keys until save() is called" in {
    deletingSessionCacheAfter {
      // Set up
      val (key, value) = testStringKeyValue
      val toSave = newCache.setting(key -> value)

      // Run test
      val restoredBeforeSave = newCache(key)
      toSave.save()
      val restoredAfterSave = newCache(key)

      // Check expectation
      restoredBeforeSave should be(None)
      restoredAfterSave should be(Some(value))
    }
  }

  "Add and delete" should "add and delete cache values" in {
    // Set up
    val cache = newCache
    val (key, value) = testStringKeyValue

    // Run tests
    val cacheWithValueSet = cache.setting(key -> value)
    val cacheWithValueRemoved = cache.removing(key)

    // Check expectations
    cache.isEmpty should be (true)
    cacheWithValueSet(key) should be (Some(value))
    cacheWithValueRemoved(key) should be (None)
  }

  "The cache key" should "contain the session ID and the word 'session'" in {
    deletingSessionCacheAfter {
      // Set up
      val sessionCache = new SessionCache(None, AppConfig.instance[SessionCacheServices])

      // Check expectations
      sessionCache.cacheKey.contains(play.mvc.Scope.Session.current().getId) should be (true)
      sessionCache.cacheKey.contains("session") should be (true)
    }
  }

  //
  // Helper methods
  //
  private def newCache: SessionCache = {
    val factory = AppConfig.instance[() => SessionCache]
    factory()
  }

  private def deletingSessionCacheAfter(operation: => Any) = {
    try {
      operation
    } finally {
      newCache.emptied.save()
    }
  }

  private def testStringKeyValue = {
    ("herp" -> "derp")
  }
}
