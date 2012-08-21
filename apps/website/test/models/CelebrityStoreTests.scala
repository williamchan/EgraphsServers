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
    val results = getPublishedCelebritiesWithIds(List(published.id, unpublished.id))

    // Check expectations
    results.map(result => result.id) should be (IndexedSeq(published.id))
  }

  "getPublishedCelebrities" should "return the celebrities in alphabetical order" in {
    // Create and persist a list of celebs whose randomly-generated names are prefixed by
    // short strings "c", "Ac", and "ab", in that order. We'll be testing if they query out
    // in the reverse order, which is what they should do if properly sorting on lower-cased
    // publicName.
    val celebs = for (namePrefix <- IndexedSeq("c", "Ac", "ab")) yield {
      val celebWithDefaultName = TestData
        .newSavedCelebrity().
        withPublishedStatus(PublishedStatus.Published)

      celebWithDefaultName.copy(publicName=namePrefix + celebWithDefaultName.publicName).save()
    }

    // Run the test
    val results = getPublishedCelebritiesWithIds(celebs.map(celeb => celeb.id))

    // Check expectations -- results should be the reverse of celebs because celebs was in reverse
    // alpha order.
    results.map(result => result.id) should be (celebs.map(celeb => celeb.id).reverse)
  }

  //
  // Private members
  //
  private def getPublishedCelebritiesWithIds(ids: Seq[Long]): IndexedSeq[Celebrity] = {
    instanceUnderTest.getPublishedCelebrities.toIndexedSeq.filter(celeb => ids.contains(celeb.id))
  }

  private def instanceUnderTest: CelebrityStore = {
    AppConfig.instance[CelebrityStore]
  }
}
