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

private[controllers] trait PostVideoAssetApiEndpoint { this: Controller =>
  protected def dbSession: DBSession

  private val blob: Blobs = AppConfig.instance[Blobs]

  def postVideoAsset = Action(parse.multipartFormData) { request =>

    request.body.file("video").map { resource =>
      import java.io.File
      val filename = resource.filename
      val contentType = resource.contentType
      val directory = "/tmp/"
      val tempFile = new File(directory + filename)
      resource.ref.moveTo(tempFile)

      putFile(filename, tempFile)

      val responseJson = Json.toJson(Map("bytes" -> Json.toJson(tempFile.length)))

      // file only needed for bytes computation
      tempFile.delete()
      Ok(responseJson)

    }.getOrElse {
      //Redirect(routes.Application.error)
      Ok("error happened")
    }
  }

  def putFile(filename: String, file: File) = {
    
    val videoKey = "videos/" + filename
    val source = Source.fromFile(file, "ISO-8859-1")
    val byteArray = source.map(_.toByte).toArray
    source.close()

    blob.put(key = videoKey, bytes = byteArray, access = AccessPolicy.Public)
  }
}