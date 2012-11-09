package models

import utils._
import services.AppConfig
import models.enums.VideoStatus

class VideoAssetTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityIdLongTests[VideoAsset]
  with CreatedUpdatedEntityTests[Long, VideoAsset]
  with DateShouldMatchers
  with DBTransactionPerTest {

  private def videoAssetStore = AppConfig.instance[VideoAssetStore]

  //
  // SavingEntityTests[VideoAsset] methods
  //
  override def newEntity = {
    val videoAsset = TestData.newSavedVideoAsset()
    VideoAsset(url = videoAsset.url)
  }

  override def saveEntity(toSave: VideoAsset) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    videoAssetStore.findById(id)
  }

  override def transformEntity(toTransform: VideoAsset) = {
    toTransform.copy(
      url = "http://www.testUrlTransformed.com")
  }

  //
  // Test cases
  //
  "A VideoAsset" should "return its associated url" in {
    val videoAsset = TestData.newSavedVideoAsset()
    val myVideoAsset = VideoAsset(url = videoAsset.url).save()
    myVideoAsset.url should be(videoAsset.url)
  }
  
  "A VideoAsset" should "update its status" in {
    val videoAsset = TestData.newSavedVideoAsset()
    videoAsset._videoStatus should be("Approved")
    val updatedVideoAsset = videoAsset.withVideoStatus(VideoStatus.Rejected).save()
    updatedVideoAsset._videoStatus should be("Rejected")
  }
}