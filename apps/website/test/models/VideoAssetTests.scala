package models

import utils._
import services.AppConfig
import models.enums.VideoStatus

class VideoAssetTests extends EgraphsUnitTest
  with ClearsCacheBefore
  with SavingEntityIdLongTests[VideoAsset]
  with CreatedUpdatedEntityTests[Long, VideoAsset]
  with DateShouldMatchers
  with DBTransactionPerTest {

  private def videoAssetStore = AppConfig.instance[VideoAssetStore]

  //
  // SavingEntityTests[VideoAsset] methods
  //
  override def newEntity = {
    VideoAsset()
  }

  override def saveEntity(toSave: VideoAsset) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    videoAssetStore.findById(id)
  }

  override def transformEntity(toTransform: VideoAsset) = {
    toTransform.copy(
      _urlKey = "videoassets/fakeId/fakeVideo.mp4")
  }

  //
  // Test cases
  //
  "A VideoAsset" should "store its associated _urlKey" in {
    val myVideoAsset = VideoAsset(_urlKey = "hiiiiiiii").save()
    myVideoAsset._urlKey should be("hiiiiiiii")
  }
  
  "A VideoAsset" should "update its status" in {
    val videoAsset = TestData.newSavedVideoAsset()
    videoAsset.videoStatus.name should be(VideoStatus.Unprocessed.name)
    val updatedVideoAsset = videoAsset.withVideoStatus(VideoStatus.Rejected).save()
    updatedVideoAsset.videoStatus.name should be(VideoStatus.Rejected.name)
  }
}