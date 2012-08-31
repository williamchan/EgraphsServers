package controllers.website

import controllers.WebsiteControllers
import org.junit.Test
import scala.collection.JavaConversions._
import play.test.FunctionalTest
import FunctionalTest._
import sjson.json.Serializer


class PostBulkEmailTests extends EgraphsFunctionalTest {
  var url = WebsiteControllers.reverse(WebsiteControllers.postSubscribeEmail).url

  @Test
  def testEmailValidation() {
    val response = POST(url,getPostStrParams(email= "", listId=""))
    println(
    response.out.toString)
    assertStatus(200, response)
    assertContentEquals(Serializer.SJSON.toJSON(
      Map("errors" ->
        Serializer.SJSON.toJSON(
          Seq("We're gonna need this", "We're gonna need this")
        )
      )
    ).toString, response)
  }

  @Test
  def testCorrectEmail() {
    val response = POST(url, getPostStrParams(email="customer@website.com", listId="2003421aassd"))
    assertStatus(200, response)
    assertContentEquals(Serializer.SJSON.toJSON(Map("subscribed" -> true)).toString, response)

  }


  private def getPostStrParams(email: String, listId: String): Map[String, String] = {
    Map[String, String]("email" -> email, "listId" -> listId)
  }
}