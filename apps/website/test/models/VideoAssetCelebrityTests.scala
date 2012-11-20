package models

import utils._
import services.AppConfig
import models.enums.VideoStatus

class VideoAssetCelebrityTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityIdLongTests[VideoAssetCelebrity]
  with DateShouldMatchers
  with DBTransactionPerTest {

  private def videoAssetCelebrityStore = AppConfig.instance[VideoAssetCelebrityStore]

  //
  // SavingEntityTests[VideoAssetCelebrity] methods
  //
  override def newEntity = {
    val videoAssetId = TestData.newSavedVideoAsset().id
    val celebrityId = TestData.newSavedCelebrity().id
    VideoAssetCelebrity(videoId = videoAssetId, celebrityId = celebrityId)
  }

  override def saveEntity(toSave: VideoAssetCelebrity) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    videoAssetCelebrityStore.findById(id)
  }

  override def transformEntity(toTransform: VideoAssetCelebrity) = {
    toTransform.copy(
      videoId = TestData.newSavedVideoAsset().id)
  }

  //
  // Test cases
  //
  "A VideoAssetCelebrity" should "return its associated celebrity and video IDs" in {
    val celebrity = TestData.newSavedCelebrity()
    val video = TestData.newSavedVideoAsset()    
    val myVideoAssetCelebrity = VideoAssetCelebrity(celebrityId = celebrity.id, videoId = video.id).save()
    myVideoAssetCelebrity.celebrityId should be(celebrity.id)
    myVideoAssetCelebrity.videoId should be (video.id)
  }
}