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

  "getPublishedCelebrities" should "return the celebrities in alphabetical order on roleDescription" in {
    // Create and persist a list of celebs whose roleDescriptions are "c", "Ac", and "ab",
    // in that order. We'll be testing if they query out in the reverse order, which is what they should do if
    // properly sorting on lower-cased roleDescription.
    val celebs = for (roleDescription <- IndexedSeq("c", "Ac", "ab")) yield {
      val celeb = TestData
        .newSavedCelebrity().
        withPublishedStatus(PublishedStatus.Published)
      celeb.copy(roleDescription = roleDescription).save()
    }

    // Run the test
    val results = getPublishedCelebritiesWithIds(celebs.map(celeb => celeb.id))

    // Check expectations -- results should be the reverse of celebs because celebs was in reverse
    // alpha order.
    results.map(result => result.id) should be (celebs.map(celeb => celeb.id).reverse)
  }

  // This test ensures that queries return results but makes no guarantees on the quality of search results
  "findByTextQuery" should "return celebrities matching the text query" in {
    val celeb = TestData
        .newSavedCelebrity().
        withPublishedStatus(PublishedStatus.Published)
    val results = instanceUnderTest.findByTextQuery(celeb.publicName)

    results.isEmpty should be(false)
  }

  // This test ensures that queries return results but makes no guarantees on the quality of search results
  "findByTextQuery" should "return  0 celebrities matching the empty string" in {
    val celeb = TestData
      .newSavedCelebrity().
      withPublishedStatus(PublishedStatus.Published)
    val results = instanceUnderTest.findByTextQuery("")

    results.isEmpty should be(true)
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
