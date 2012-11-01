package controllers

import play.api._
import play.api.mvc._
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.libs.iteratee.Enumerator

object Application extends Controller {

  def ping(code: Long) = Action {
    SimpleResult(
      header = ResponseHeader(code.toInt, Map(CONTENT_TYPE -> "text/plain")),
      body = Enumerator("The response status was " + code))
  }

  def uploadTest = Action {
    Ok(views.html.upload())
  }

  def upload = Action(parse.multipartFormData) { request =>

    request.body.file("video").map { resource =>
      import java.io.File
      val filename = resource.filename
      val contentType = resource.contentType
      val directory = "/tmp/"
      val tempFile = new File(directory + filename)
      resource.ref.moveTo(tempFile)

      val responseJson = Json.toJson(Map("bytes" -> Json.toJson(tempFile.length)))

      // file only needed for bytes computation
      tempFile.delete()
      Ok(responseJson)

    }.getOrElse {
      Redirect(routes.Application.error)
    }
  }

  def error = Action {
    Ok("Missing file!")
  }

}