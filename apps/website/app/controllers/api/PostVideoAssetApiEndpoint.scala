package controllers.api

import services.db.DBSession
import play.api._
import play.api.mvc._
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import java.io.File
import services.blobs.Blobs
import services.AppConfig
import scala.io.Source
import services.blobs.AccessPolicy
import play.api.data.Forms._
import play.api.mvc.MultipartFormData.FilePart

private[controllers] trait PostVideoAssetApiEndpoint { this: Controller =>
  protected def dbSession: DBSession

  private val blob: Blobs = AppConfig.instance[Blobs]

  def postVideoAsset = Action(parse.multipartFormData) { request =>

    // get celebrity ID out of post request 
    val maybeCelebrityId = request.body.dataParts.get("celebrityId")
    val celebrityId = maybeCelebrityId match {
      case Some(maybeCelebrityId) => maybeCelebrityId(0)
      case None => ""
    }

    request.body.file("video").map { resource =>
      import java.io.File
      val filename = resource.filename
      val directory = "/tmp/"
      val tempFile = new File(directory + filename)
      resource.ref.moveTo(tempFile)

      val maybeFileLocation = putFile(celebrityId, filename, tempFile)
      val fileLocation = maybeFileLocation match {
        case Some(maybeFileLocation) => maybeFileLocation
        case None => "File not found"
      }

      // add row to videoasset table
      // add row to celebvideoasset table
      // make sure to not send responseJson unless db update goes through

      val responseJson = getJson(celebrityId, tempFile, fileLocation)

      // file only needed for bytes computation
      tempFile.delete()
      Ok(responseJson)

    }.getOrElse {
      BadRequest("Something went wrong with your request. Please try again.")
    }
  }

  private def putFile(celebrityId: String, filename: String, file: File): Option[String] = {

    val videoKey = "videos/" + celebrityId + "/" + filename
    val source = Source.fromFile(file, "ISO-8859-1")
    val byteArray = source.map(_.toByte).toArray
    source.close()

    blob.put(key = videoKey, bytes = byteArray, access = AccessPolicy.Public)
    blob.getUrlOption(key = videoKey)
  }

  private def getJson(celebrityId: String, tempFile: java.io.File, maybeFileLocation: String) = {
    val jsonIterable = Json.toJson(Map(
      "celebrityId" -> Json.toJson(celebrityId),
      "bytes" -> Json.toJson(tempFile.length),
      "url" -> Json.toJson(maybeFileLocation)))

    Json.toJson(Map("video" -> jsonIterable))
  }
}