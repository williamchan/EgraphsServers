package controllers.utils

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
import play.api.mvc.Controller

trait PostVideoAssetHelper { this: Controller =>
  protected def httpFilters: HttpFilters
  protected def blobs: Blobs

  protected def putFile(celebrity: Celebrity, filename: String, file: File): Option[String] = {

    val videoKey = "videos/" + celebrity.id + "/" + filename
    val source = Source.fromFile(file, "ISO-8859-1")
    val byteArray = source.map(_.toByte).toArray
    source.close()

    blobs.put(key = videoKey, bytes = byteArray, access = AccessPolicy.Private)
    blobs.getUrlOption(key = videoKey)
  }

  protected def getJson(celebrity: Celebrity, tempFile: java.io.File, maybeFileLocation: String) = {
    val jsonIterable = Json.toJson(Map(
      "celebrityId" -> Json.toJson(celebrity.id),
      "bytes" -> Json.toJson(tempFile.length),
      "url" -> Json.toJson(maybeFileLocation)))

    Json.toJson(Map("video" -> jsonIterable))
  }

  protected def persist(celebrity: Celebrity, fileLocation: String) = {
    // add row to videoAssets table
    val videoAsset = VideoAsset(url = fileLocation).save()
    val videoId = videoAsset.id

    // add row to videoAssetsCelebrity table
    val videoAssetCelebrity = VideoAssetCelebrity(
      celebrityId = celebrity.id,
      videoId = videoId)
    videoAssetCelebrity.save()
  }

  protected def postVideoAssetBase = {

    httpFilters.requireCelebrityId.inRequest(parse.multipartFormData) { celebrity =>
      Action(parse.multipartFormData) { request =>

        request.body.file("video").map { resource =>

          val filename = resource.filename
          val tempFile = TempFile.named(filename)

          resource.ref.moveTo(tempFile, true)

          val maybeFileLocation = putFile(celebrity, filename, tempFile)
          maybeFileLocation match {
            case Some(maybeFileLocation) => {
              persist(celebrity, maybeFileLocation)
              val responseJson = getJson(celebrity, tempFile, maybeFileLocation)
              Ok(responseJson)
            }
            case None => InternalServerError("The video was not found")
          }

        }.getOrElse {
          BadRequest("Something went wrong with your request. Please try again.")
        }
      }
    }
  }
}