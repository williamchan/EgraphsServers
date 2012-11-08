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
  "videoAsset" should "return the associated VideoAsset" in {
    val videoAsset = TestData.newSavedVideoAsset()
    val myVideoAsset = VideoAsset(url = videoAsset.url).save()
    myVideoAsset.url should be(videoAsset.url)
  }
  
  "videoAsset" should "update its status" in {
    val videoAsset = TestData.newSavedVideoAsset()
    videoAsset._videoStatus should be("Unprocessed")
    videoAsset.withVideoStatus(VideoStatus.Approved)
    videoAsset._videoStatus should be ("Approved")
  }
}