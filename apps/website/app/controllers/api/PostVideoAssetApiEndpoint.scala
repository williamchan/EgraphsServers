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
    val body = request.body.dataParts.get("celebrityId")
    val maybeCelebrityId = body match {
      case Some(body) => body(0)
      case None => ""
    }

    // remove after testing, probably
    play.Logger.info("celebrityId is " + maybeCelebrityId)

    request.body.file("video").map { resource =>
      import java.io.File
      val filename = resource.filename
      val directory = "/tmp/"
      val tempFile = new File(directory + filename)
      resource.ref.moveTo(tempFile)

      val maybeFileLocation = putFile(filename, tempFile)
      maybeFileLocation match {
        case Some(maybeFileLocation) => play.Logger.info("File location is " + maybeFileLocation)
        case None => play.Logger.info("Oops, couldn't file the file!")
      }

      // add row to videoasset table
      // add row to celebvideoasset table
      
      val jsonIterable = Json.toJson(Map(
          "bytes" -> Json.toJson(tempFile.length),
          "url" -> Json.toJson(maybeFileLocation)))
      
      val responseJson = Json.toJson(Map("video" -> jsonIterable))

      // file only needed for bytes computation
      tempFile.delete()
      Ok(responseJson)

    }.getOrElse {
      //Redirect(routes.Application.error)
      Ok("error happened")
    }
  }

  private def putFile(filename: String, file: File): Option[String] = {

    val videoKey = "videos/" + filename
    val source = Source.fromFile(file, "ISO-8859-1")
    val byteArray = source.map(_.toByte).toArray
    source.close()

    blob.put(key = videoKey, bytes = byteArray, access = AccessPolicy.Public)
    blob.getUrlOption(key = videoKey)
    
    // HOW TO FIND FILE IN S3
    //http://s3.amazonaws.com/YOUR-BUCKET-NAME/YOUR-FILE-NAME
  }
}