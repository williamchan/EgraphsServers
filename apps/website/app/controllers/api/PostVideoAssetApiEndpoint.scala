package controllers.api

import java.io.File
import scala.io.Source
import models.VideoAsset
import models.VideoAssetCelebrity
import play.api.libs.json.Json
import play.api.mvc.Controller
import play.api.mvc.Action
import services.blobs.AccessPolicy
import services.blobs.Blobs
import services.http.filters.HttpFilters
import services.http.POSTApiControllerMethod
import services.AppConfig
import models.VideoAsset
import models.VideoAssetCelebrity

private[controllers] trait PostVideoAssetApiEndpoint { this: Controller =>
  protected def postApiController: POSTApiControllerMethod
  protected def httpFilters: HttpFilters

  private val blob: Blobs = AppConfig.instance[Blobs]

  def postVideoAsset = postApiController() {
    httpFilters.requireAdministratorLogin.inSession(parser = parse.multipartFormData) {
      case (admin, adminAccount) =>
        httpFilters.requireCelebrityId.inRequest(parse.multipartFormData) { celebrity =>
          Action(parse.multipartFormData) { request =>

            val celebrityId = celebrity.id

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

              persist(celebrityId, fileLocation)
              val responseJson = getJson(celebrityId, tempFile, fileLocation)

              // file only needed for bytes computation
              tempFile.delete()
              Ok(responseJson)

            }.getOrElse {
              BadRequest("Something went wrong with your request. Please try again.")
            }
          }
        }
    }
  }

  private def putFile(celebrityId: Long, filename: String, file: File): Option[String] = {

    val videoKey = "videos/" + celebrityId + "/" + filename
    val source = Source.fromFile(file, "ISO-8859-1")
    val byteArray = source.map(_.toByte).toArray
    source.close()

    blob.put(key = videoKey, bytes = byteArray, access = AccessPolicy.Public)
    blob.getUrlOption(key = videoKey)
  }

  private def getJson(celebrityId: Long, tempFile: java.io.File, maybeFileLocation: String) = {
    val jsonIterable = Json.toJson(Map(
      "celebrityId" -> Json.toJson(celebrityId),
      "bytes" -> Json.toJson(tempFile.length),
      "url" -> Json.toJson(maybeFileLocation)))

    Json.toJson(Map("video" -> jsonIterable))
  }

  private def persist(celebrityId: Long, fileLocation: String) = {
    // add row to videoAssets table
    val videoAsset = VideoAsset(url = fileLocation).save()
    val videoId = videoAsset.id

    // add row to videoAssetsCelebrity table
    val videoAssetCelebrity = VideoAssetCelebrity(
      celebrityId = celebrityId,
      videoId = videoId)
    videoAssetCelebrity.save()
  }
}