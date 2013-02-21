package services.video

import java.io.File

import scala.concurrent.Future

import play.api.Play.current
import play.api.libs.Files.TemporaryFile
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results.Async
import play.api.mvc.Results.BadRequest
import play.api.mvc.Results.NotFound
import play.api.mvc.Results.Ok
import play.api.mvc.MultipartFormData
import play.api.mvc.Request
import play.api.mvc.Result
import models.Celebrity
import models.VideoAsset
import models.VideoAssetCelebrity

import services.blobs.AccessPolicy
import services.blobs.Blobs
import services.db.DBSession
import services.db.TransactionSerializable
import services.http.filters.HttpFilters
import services.TempFile

trait PostVideoAssetHelper {
  protected def httpFilters: HttpFilters
  protected def blobs: Blobs
  protected def dbSession: DBSession

  protected def putFile(blobKey: String, filename: String, file: File): Option[String] = {
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

  protected def postSaveVideoAssetToS3AndDBAction(
    implicit request: Request[MultipartFormData[TemporaryFile]]): Result = {

    val errorOrCelebrity = dbSession.connected(TransactionSerializable) {
      httpFilters.requireCelebrityId.filterInRequest(parse.multipartFormData)
    }
    
    errorOrCelebrity.fold(
      error => error, {
        case celebrity =>

          request.body.file("video").map { resource =>

            val filename = resource.filename
            val tempFile = TempFile.named(filename)

            val blobKey = dbSession.connected(TransactionSerializable) {
              persist(celebrity, filename)
            }

            val promiseOfMaybeFileLocation: Future[Option[String]] = Akka.future {
              resource.ref.moveTo(tempFile, true)
              putFile(blobKey, filename, tempFile)
            }

            Async {
              promiseOfMaybeFileLocation.map { maybeFileLocation =>
                maybeFileLocation match {
                  case None => NotFound("The video " + filename + " was not found")
                  case Some(fileLocation) => {
                    val responseJson = getJson(celebrity, tempFile, fileLocation)
                    Ok(responseJson)
                  }
                }
              }
            }
          }.getOrElse {
            BadRequest("Something went wrong with your request. Please try again.")
          }
      })
  }
}