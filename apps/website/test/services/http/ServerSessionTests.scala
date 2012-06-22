package services.http

import utils.EgraphsUnitTest
import services.AppConfig

class ServerSessionTests extends EgraphsUnitTest {
  "The server session" should "start out empty" in {
    newSession.isEmpty should be (true)
  }

  "Saving a populated session" should "work" in {
    deletingSessionAfter {
      // Set up
      val (key, value) = testStringKeyValue
      val session = newSession

      // Run test
      val valueBeforeSave = session(key)
      session.setting(key -> value).save()
      val valueAfterSave = newSession(key)

      // Check expectations
      valueBeforeSave should be (None)
      valueAfterSave should be (Some(value))
    }
  }

  "Saving an emptied session" should "actually delete keys" in {
    deletingSessionAfter {
      // Set up
      val (key, value) = testStringKeyValue
      val session = newSession
      val savedSession = session.setting(key -> value).save()

      // Run test
      val restoredBeforeEmpty = newSession(key)
      savedSession.emptied.save()
      val restoredAfterEmpty = newSession(key)

      // Check expectations
      restoredBeforeEmpty should be (Some(value))
      restoredAfterEmpty should be (None)
    }
  }

  "The session session" should "not save keys until save() is called" in {
    deletingSessionAfter {
      // Set up
      val (key, value) = testStringKeyValue
      val toSave = newSession.setting(key -> value)

      // Run test
      val restoredBeforeSave = newSession(key)
      toSave.save()
      val restoredAfterSave = newSession(key)

      // Check expectation
      restoredBeforeSave should be(None)
      restoredAfterSave should be(Some(value))
    }
  }

  "Add and delete" should "add and delete session values" in {
    // Set up
    val session = newSession
    val (key, value) = testStringKeyValue

    // Run tests
    val sessionWithValueSet = session.setting(key -> value)
    val sessionWithValueRemoved = session.removing(key)

    // Check expectations
    session.isEmpty should be (true)
    sessionWithValueSet(key) should be (Some(value))
    sessionWithValueRemoved(key) should be (None)
  }

  "The session key" should "contain the session ID and the word 'session'" in {
    deletingSessionAfter {
      // Set up
      val session = new ServerSession(None, AppConfig.instance[ServerSessionServices])

      // Check expectations
      session.cacheKey.contains(play.mvc.Scope.Session.current().getId) should be (true)
      session.cacheKey.contains("session") should be (true)
    }
  }

  //
  // Helper methods
  //
  private def newSession: ServerSession = {
    val factory = AppConfig.instance[() => ServerSession]
    factory()
  }

  private def deletingSessionAfter(operation: => Any) = {
    try {
      operation
    } finally {
      newSession.emptied.save()
    }
  }

  private def testStringKeyValue = {
    ("herp" -> "derp")
  }
}
