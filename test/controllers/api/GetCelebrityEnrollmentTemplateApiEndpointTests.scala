package controllers.api

import org.junit.Assert._
import org.junit.Test
import play.test.FunctionalTest
import sjson.json.Serializer
import utils.FunctionalTestUtils.{CleanDatabaseAfterEachTest, willChanRequest, runScenario}
import utils.TestConstants

class GetCelebrityEnrollmentTemplateApiEndpointTests extends FunctionalTest with CleanDatabaseAfterEachTest {

  import FunctionalTest._

  @Test
  def testGettingACelebrityEnrollmentTemplate() {
    runScenario("Will-Chan-is-a-celebrity")

    val response = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/enrollmenttemplate")
    assertIsOk(response)
    val json = Serializer.SJSON.in[Map[String, List[String]]](getContent(response))
    assertEquals(1, json.size)
    val enrollmentPhrases: List[String] = json("enrollmentPhrases")
    assertEquals(20, enrollmentPhrases.size)
    assertEquals("My name is Wizzle", enrollmentPhrases(0))
    assertEquals("One, two, three, four, five", enrollmentPhrases(1))
    assertEquals("Stop each car if it's little", enrollmentPhrases(2))
    assertEquals("Play in the street up ahead", enrollmentPhrases(3))
    assertEquals("A fifth wheel caught speeding", enrollmentPhrases(4))
    assertEquals("It's been about two years since Davey kept shotguns", enrollmentPhrases(5))
    assertEquals("Charlie did you think to measure the tree", enrollmentPhrases(6))
    assertEquals("Tina got cued to make a quicker escape", enrollmentPhrases(7))
    assertEquals("Joe books very few judges", enrollmentPhrases(8))
    assertEquals("Here I was in Miami and Illinois", enrollmentPhrases(9))
    assertEquals("Stop each car if it's little", enrollmentPhrases(10))
    assertEquals("Play in the street up ahead", enrollmentPhrases(11))
    assertEquals("A fifth wheel caught speeding", enrollmentPhrases(12))
    assertEquals("It's been about two years since Davey kept shotguns", enrollmentPhrases(13))
    assertEquals("Charlie did you think to measure the tree", enrollmentPhrases(14))
    assertEquals("Tina got cued to make a quicker escape", enrollmentPhrases(15))
    assertEquals("Joe books very few judges", enrollmentPhrases(16))
    assertEquals("Here I was in Miami and Illinois", enrollmentPhrases(17))
    assertEquals("Six, seven, eight, nine, ten", enrollmentPhrases(18))
    assertEquals("My name is Wizzle", enrollmentPhrases(19))
  }
}
