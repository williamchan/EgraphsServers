package services.http

import utils.EgraphsUnitTest
import services.AppConfig
import play.mvc.Scope
import play.mvc.Scope.Session

class EgraphsSessionTests extends EgraphsUnitTest {
  import EgraphsSession.Key

  "get" should "return the underlying value of an play Session when no new value is set" in {
    // Set up
    val playSession = new Scope.Session()
    val underTest = makeEgraphsSession(playSession)
    val key = Key.AdminId
    val value = "herp"

    // Run test
    // 1. Query a value
    val valueBeforeInsert = underTest(key)

    // 2. Insert one into that key and query it back from both the inserted and a new instance
    val underTestInsertedNotSaved = underTest.withString(key -> value)
    val valueFromSameInstanceAfterInsertButNotSave = underTestInsertedNotSaved(key)
    val valueFromNewInstanceAfterInsertButNotSave = makeEgraphsSession(playSession)(key)

    // 3. Save the inserted instance and query out a new instance
    val valueFromOldInstanceAfterSave = underTestInsertedNotSaved.save()(key)
    val valueFromNewInstanceAfterSave = makeEgraphsSession(playSession)(key)

    // Check expectations
    valueBeforeInsert should be (None)

    valueFromNewInstanceAfterInsertButNotSave should be (None)
    valueFromSameInstanceAfterInsertButNotSave should be (Some(value))

    valueFromNewInstanceAfterSave should be (Some(value))
    valueFromOldInstanceAfterSave should be (Some(value))
  }

  "remove" should "remove instances on save" in {
   // Set up
    val playSession = new Scope.Session()
    val (key, value) = Key.AdminId -> "derp"
    playSession.put(key.name, value)

    val underTest = makeEgraphsSession(playSession)

    // Run test
    val valueBeforeRemove = underTest(key)
    val valueAfterRemove = underTest.deleting(key).save()(key)

    // Check expectation
    valueBeforeRemove should be (Some(value))
    valueAfterRemove should be (None)
    playSession.get(key.name) should be (null)
  }

  "getLongOption" should "return long values" in {
    // Set up
    val playSession = new Scope.Session()
    playSession.put(Key.AdminId.name, "10")

    // Run test
    val underTest = makeEgraphsSession(playSession)

    // Check expectation
    underTest.getLong(Key.AdminId) should be (Some(10L))
  }

  "withLong" should "set a long" in {
    // Set up
    val playSession = new Scope.Session()
    val underTest = makeEgraphsSession(playSession)

    // Run test
    underTest.withLong(Key.AdminId -> 10L).save()

    // Check expectation
    playSession.get(Key.AdminId.name) should be ("10")
  }

  "cleared" should "clear all values" in {

  }

  private def makeEgraphsSession(session: Session): EgraphsSession = {
    val services = sessionServices.copy(playSessionFactory = () => session)
    new EgraphsSession(Map(), services)
  }

  private def sessionServices: EgraphsSessionServices = {
    AppConfig.instance[EgraphsSessionServices]
  }
}
