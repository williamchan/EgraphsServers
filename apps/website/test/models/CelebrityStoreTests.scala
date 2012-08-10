package models

import enums.PublishedStatus
import utils.{DBTransactionPerTest, TestData, EgraphsUnitTest}
import services.AppConfig

class CelebrityStoreTests extends EgraphsUnitTest with DBTransactionPerTest {
  "getPublishedCelebrities" should "get any celebrities that are published and none that are unpublished" in {
    // Set up a published and unpublished celebrity
    val published = TestData.newSavedCelebrity()
      .withPublishedStatus(PublishedStatus.Published)
      .save()

    val unpublished = TestData.newSavedCelebrity()
      .withPublishedStatus(PublishedStatus.Unpublished)
      .save()

    // Query out the published ones but restrict to the ids of our test entities.
    val resultIds = instanceUnderTest.getPublishedCelebrities
      .toList
      .filter(celeb => List(published.id, unpublished.id).contains(celeb.id))
      .map(celeb => celeb.id)

    // Check expectations
    resultIds should be (List(published.id))
  }

  //
  // Private members
  //
  def instanceUnderTest: CelebrityStore = {
    AppConfig.instance[CelebrityStore]
  }
}
