package services.video

import java.io.File
import scala.io.Source
import models.Celebrity
import models.VideoAsset
import models.VideoAssetCelebrity
import play.api.libs.json.Json
import services.blobs.AccessPolicy
import services.blobs.Blobs
import services.TempFile
import services.http.filters.HttpFilters
import play.api.mvc.Action
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Result
import play.api.mvc.Results.{ Ok, BadRequest, NotFound, Async }
import play.api.mvc.Controller
import play.api.mvc.MultipartFormData
import play.api.libs.Files.TemporaryFile
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.concurrent.Promise
import services.db.DBSession
import services.db.TransactionSerializable

trait PostVideoAssetHelper {
  protected def httpFilters: HttpFilters
  protected def blobs: Blobs
  protected def dbSession: DBSession

  protected def putFile(blobKey: String, filename: String, file: File): Option[String] = {
    
    println("blobKey is " + blobKey)
    
    val byteArray = Blobs.Conversions.fileToByteArray(file)
    blobs.put(key = blobKey, bytes = byteArray, access = AccessPolicy.Private)
    blobs.getUrlOption(key = blobKey)
  }

  protected def getJson(celebrity: Celebrity, tempFile: java.io.File, maybeFileLocation: String) = {
    val jsonIterable = Json.toJson(Map(
      "celebrityId" -> Json.toJson(celebrity.id),
      "bytes" -> Json.toJson(tempFile.length),
      "url" -> Json.toJson(maybeFileLocation)))

    Json.toJson(Map("video" -> jsonIterable))
  }

  protected def persist(celebrity: Celebrity, filename: String): String = {
    // add row to videoAssets table
    val videoAsset = VideoAsset().save()
    val blobKey = videoAsset.setVideoUrlKey(filename)

    // add row to videoAssetsCelebrity table
    VideoAssetCelebrity(
      celebrityId = celebrity.id,
      videoId = videoAsset.id).save()

    blobKey
  }

  protected def postSaveVideoAssetToS3AndDBAction: Action[MultipartFormData[TemporaryFile]] = {

    httpFilters.requireCelebrityId.inRequest(parse.multipartFormData) { celebrity =>
      Action(parse.multipartFormData) { request =>

        request.body.file("video").map { resource =>

          val filename = resource.filename
          val tempFile = TempFile.named(filename)

          resource.ref.moveTo(tempFile, true)
          val blobKey = persist(celebrity, filename)
          val maybeFileLocation = putFile(blobKey, filename, tempFile)

          maybeFileLocation match {
            case None => NotFound("The video " + filename + " was not found")
            case Some(fileLocation) => {
              val responseJson = getJson(celebrity, tempFile, fileLocation)
              Ok(responseJson)
            }
          }
        }.getOrElse {
          BadRequest("Something went wrong with your request. Please try again.")
        }
      }
    }
  }
}