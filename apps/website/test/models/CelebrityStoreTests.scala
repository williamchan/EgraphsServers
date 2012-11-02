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
    val celeb = TestData.newSavedCelebrity().withPublishedStatus(PublishedStatus.Published)
    val results = instanceUnderTest.findByTextQuery(celeb.publicName)

    results.isEmpty should be(false)
  }

  // This test ensures that queries return results but makes no guarantees on the quality of search results
  "findByTextQuery" should "return 0 celebrities matching the empty string" in {
    TestData.newSavedCelebrity().withPublishedStatus(PublishedStatus.Published)
    val results = instanceUnderTest.findByTextQuery("")

    results.isEmpty should be(true)
  }
  
  "find by filter value" should "return celebrities associated with a particular filter value" in new EgraphsTestApplication {
 
    val filter = TestData.newSavedFilter
    val filterValueA = TestData.newSavedFilterValue(filter.id)
    val filterValueB = TestData.newSavedFilterValue(filter.id)

    val celebsTaggedWithA = for ( i <- 0 until 10) yield {
     TestData.newSavedCelebrity()
     	.withPublishedStatus(PublishedStatus.Published)
     	.save()
    }
    
    val celebsTaggedWithB = for ( i <- 0 until 10) yield {
     TestData.newSavedCelebrity()
     	.withPublishedStatus(PublishedStatus.Published)
     	.save()
    }
    
    celebsTaggedWithA.map(celeb => celeb.filterValues.associate(filterValueA))
    celebsTaggedWithB.map(celeb => celeb.filterValues.associate(filterValueB))
  
    val retrievedFilterValuesA = instanceUnderTest.findByFilterValueId(filterValueA.id)
    val retrievedFilterValuesB = instanceUnderTest.findByFilterValueId(filterValueB.id)
    
    // Returns the set associated with the filter
    retrievedFilterValuesA.map(celeb => celebsTaggedWithA should contain(celeb))
    
   retrievedFilterValuesB.map(celeb => celebsTaggedWithB should contain(celeb))

    // And the set is exclusive of the other filter
    retrievedFilterValuesA.map(celeb => celebsTaggedWithB.exists(c => celeb.id == c.id) should be(false))
    retrievedFilterValuesB.map(celeb => celebsTaggedWithA.exists(c => celeb.id == c.id) should be(false))    
  }

  // Private members
  //
  private def getPublishedCelebritiesWithIds(ids: Seq[Long]): IndexedSeq[Celebrity] = {
    instanceUnderTest.getPublishedCelebrities.toIndexedSeq.filter(celeb => ids.contains(celeb.id))
  }

  private def instanceUnderTest: CelebrityStore = {
    AppConfig.instance[CelebrityStore]
  }
}
