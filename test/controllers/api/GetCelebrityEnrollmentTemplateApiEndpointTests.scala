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
    val _enrollmentPhrases: String = GetCelebrityEnrollmentTemplateApiEndpoint._enrollmentPhrases
    val _text: String = GetCelebrityEnrollmentTemplateApiEndpoint._text

    runScenario("Will-Chan-is-a-celebrity")

    val response = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/enrollmenttemplate")
    assertIsOk(response)
    val json = Serializer.SJSON.in[Map[String, List[Map[String, String]]]](getContent(response))
    assertEquals(1, json.size)
    val enrollmentPhrases: List[Map[String, String]] = json(_enrollmentPhrases)
    assertEquals(20, enrollmentPhrases.size)
    assertEquals("My name is Wizzle", enrollmentPhrases(0)(_text))
    assertEquals("One, two, three, four, five", enrollmentPhrases(1)(_text))
    assertEquals("Stop each car if it's little", enrollmentPhrases(2)(_text))
    assertEquals("Play in the street up ahead", enrollmentPhrases(3)(_text))
    assertEquals("A fifth wheel caught speeding", enrollmentPhrases(4)(_text))
    assertEquals("It's been about two years since Davey kept shotguns", enrollmentPhrases(5)(_text))
    assertEquals("Charlie did you think to measure the tree", enrollmentPhrases(6)(_text))
    assertEquals("Tina got cued to make a quicker escape", enrollmentPhrases(7)(_text))
    assertEquals("Joe books very few judges", enrollmentPhrases(8)(_text))
    assertEquals("Here I was in Miami and Illinois", enrollmentPhrases(9)(_text))
    assertEquals("Stop each car if it's little", enrollmentPhrases(10)(_text))
    assertEquals("Play in the street up ahead", enrollmentPhrases(11)(_text))
    assertEquals("A fifth wheel caught speeding", enrollmentPhrases(12)(_text))
    assertEquals("It's been about two years since Davey kept shotguns", enrollmentPhrases(13)(_text))
    assertEquals("Charlie did you think to measure the tree", enrollmentPhrases(14)(_text))
    assertEquals("Tina got cued to make a quicker escape", enrollmentPhrases(15)(_text))
    assertEquals("Joe books very few judges", enrollmentPhrases(16)(_text))
    assertEquals("Here I was in Miami and Illinois", enrollmentPhrases(17)(_text))
    assertEquals("Six, seven, eight, nine, ten", enrollmentPhrases(18)(_text))
    assertEquals("My name is Wizzle", enrollmentPhrases(19)(_text))
  }
}
