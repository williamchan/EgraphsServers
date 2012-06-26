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
      val session = new ServerSession(None)

      // Check expectations
      session.cacheKey.contains(play.mvc.Scope.Session.current().getId) should be (true)
      session.cacheKey.contains("session") should be (true)
    }
  }

  "namespacing" should "get and set on the namespace correctly" in {
    // Set up
    deletingSessionAfter {
      setHerpAndNsHerpIntoSession

      // Check expectations: herp and ns/herp should be different
      newSession.get("herp") should be (Some("derp"))
      newNamespacedSession.get("herp") should be (Some("ns-derp"))
      newSession.get("ns/herp") should be (newNamespacedSession.get("herp"))

      /*// Re-add the namespaced one, delete the original and see that the namespaced exists
      newSession.namespaced("ns").setting("derp-ns" -> ).save()
      newSession.namespaced("")
      newSession.get("herp") should be (Some(""))*/
    }
  }

  "deleting from a namespace" should "not affect the upper namespaces" in {
    deletingSessionAfter {
      setHerpAndNsHerpIntoSession

      newNamespacedSession.removing("herp").save()
      newNamespacedSession.get("herp") should be (None)
      newSession.get("herp") should be (Some("derp"))
    }
  }

  "namespacing" should "apply to arbitrary depth" in {
    deletingSessionAfter {
      newSession
        .namespaced("1")
        .namespaced("2")
        .setting("herp" -> "derp")
        .save()

      newSession.get("1/2/herp") should be (Some("derp"))
    }
  }

  "namespacing" should "empty from the namespace correctly" in {
    deletingSessionAfter {
      setHerpAndNsHerpIntoSession
      newNamespacedSession.setting("just another key" -> "and value").save()
      newNamespacedSession.size should be (2)
      newNamespacedSession.emptied.save()
      newNamespacedSession.size should be (0)
      newSession.size should be (1)
      newNamespacedSession.isEmpty should be (true)
    }
  }

  "namespaces" should "return the correct server session namespaces" in {
    deletingSessionAfter {
      // Create a root namespace and 4 sub-namespaces
      def noNamespace = newSession
      def ns1 = newSession.namespaced("ns1")
      def ns2 = ns1.namespaced("ns2")
      def ns3 = ns2.namespaced("ns3")

      // Add "namespace name" key into each of the 4 namespaces
      for (namespacedSession <- List(noNamespace, ns1, ns2, ns3)) {
        namespacedSession.setting("namespace name" -> namespacedSession.namespace).save()
      }

      // Verify that each namespace only reports its own direct child namespaces
      noNamespace.namespaces.map(_.namespace).toSet should be (Set("ns1"))
      ns1.namespaces.map(_.namespace).toSet should be (Set("ns1/ns2"))
      ns2.namespaces.map(_.namespace).toSet should be (Set("ns1/ns2/ns3"))
      ns3.namespaces.isEmpty should be(true)
    }
  }

  //
  // Helper methods
  //
  private def newSession: ServerSession = {
    val factory = AppConfig.instance[() => ServerSession]
    factory()
  }

  private def newNamespacedSession = {
    newSession.namespaced("ns")
  }
  private def setHerpAndNsHerpIntoSession() = {
    newSession.setting("herp" -> "derp")
      .namespaced("ns")
      .setting("herp" -> "ns-derp")
      .save()
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
