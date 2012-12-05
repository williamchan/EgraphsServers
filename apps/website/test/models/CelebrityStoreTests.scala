package models

import enums.{PublishedStatus, EnrollmentStatus}
import utils.{DBTransactionPerTest, TestData, EgraphsUnitTest}
import services.AppConfig
import services.mvc.celebrity.CatalogStarsQuery
import services.db.DBSession
import services.db.Schema
import models.frontend.landing.CatalogStar
import akka.agent.Agent
import play.api.libs.concurrent.Akka
import services.mvc.celebrity.UpdateCatalogStarsActor
import services.cache.CacheFactory
import services.mvc.celebrity.CelebrityViewConverting

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

  it should "return the celebrities in alphabetical order on roleDescription" in {
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
  "celebritiesSearch" should "return celebrities matching the text query" in {
    val celeb = newSearchableCeleb
    instanceUnderTest.rebuildSearchIndex
    val results = instanceUnderTest.celebritiesSearch(Some(celeb.publicName))

    results.isEmpty should be(false)
  }

  // This test ensures that queries return results but makes no guarantees on the quality of search results
  it should "return 0 celebrities matching the empty string" in {
    val celeb = newSearchableCeleb
    instanceUnderTest.rebuildSearchIndex
    val results = instanceUnderTest.celebritiesSearch(Some(""))

    results.isEmpty should be(true)
  }

  it should "actually rebuild the search index" in {
    val celeb = newSearchableCeleb
    val results = instanceUnderTest.celebritiesSearch(Some(celeb.publicName))
    results.isEmpty should be(true)

    instanceUnderTest.rebuildSearchIndex
    val results1 = instanceUnderTest.celebritiesSearch(Some(celeb.publicName))
    results1.isEmpty should be(false)
  }
  
  it should "find celebs tagged with a specific value" in {
    val category = TestData.newSavedCategory
    val categoryValueA = TestData.newSavedCategoryValue(category.id)
    val celeb = newSearchableCeleb
    celeb.categoryValues.associate(categoryValueA)

    instanceUnderTest.rebuildSearchIndex
    val results1 = instanceUnderTest.celebritiesSearch(Some(categoryValueA.publicName))
    results1.isEmpty should be(false)
  }

  it should "find celebs tagged with two values" in {
    val category = TestData.newSavedCategory
    val categoryValueA = TestData.newSavedCategoryValue(category.id)
    val categoryValueB = TestData.newSavedCategoryValue(category.id)
    val celeb = newSearchableCeleb
    celeb.categoryValues.associate(categoryValueA)
    celeb.categoryValues.associate(categoryValueB)

    instanceUnderTest.rebuildSearchIndex
    val results1 = instanceUnderTest.celebritiesSearch(Some(categoryValueA.publicName + " " + categoryValueB.publicName))
    results1.isEmpty should be(false)
  }

  it should "find celebs of a certain category through refinements" in {
    val category = TestData.newSavedCategory
    val categoryValueA = TestData.newSavedCategoryValue(category.id)
    val celeb = newSearchableCeleb
    celeb.categoryValues.associate(categoryValueA)

    instanceUnderTest.rebuildSearchIndex
    val results1 = instanceUnderTest.celebritiesSearch(maybeQuery = None, refinements = List(Set(categoryValueA.id)))
    results1.isEmpty should be(false)
  }

  it should "find celebs of a certain category through multiple refinements in the same category (OR search)" in {
    val category = TestData.newSavedCategory
    
    val categoryValueA = TestData.newSavedCategoryValue(category.id)
    val celebA = newSearchableCeleb
    celebA.categoryValues.associate(categoryValueA)

    val categoryValueB = TestData.newSavedCategoryValue(category.id)
    val celebB = newSearchableCeleb
    celebB.categoryValues.associate(categoryValueB)

    instanceUnderTest.rebuildSearchIndex
    val results1 = instanceUnderTest.celebritiesSearch(maybeQuery = None, refinements = List(Set(categoryValueA.id, categoryValueB.id)))
    results1.size should be(2)
  }

  it should "find celebs of a certain category through multiple refinements in different categories (AND search)" in {
    val categoryA = TestData.newSavedCategory
    
    val categoryValueA = TestData.newSavedCategoryValue(categoryA.id)
    val celebA = newSearchableCeleb
    celebA.categoryValues.associate(categoryValueA)

    val categoryB = TestData.newSavedCategory
    val categoryValueB = TestData.newSavedCategoryValue(categoryB.id)
    val celebAB = newSearchableCeleb
    celebAB.categoryValues.associate(categoryValueB)
    celebAB.categoryValues.associate(categoryValueA)

    instanceUnderTest.rebuildSearchIndex
    val results = instanceUnderTest.celebritiesSearch(maybeQuery = None, refinements = 
      List(Set(categoryValueA.id), 
           Set(categoryValueB.id)
      ))
    results.size should be(1)
  }

  "find by category value" should "return celebrities associated with a particular CategoryValue" in new EgraphsTestApplication {
 
    val category = TestData.newSavedCategory
    val categoryValueA = TestData.newSavedCategoryValue(category.id)
    val categoryValueB = TestData.newSavedCategoryValue(category.id)

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
    
    celebsTaggedWithA.map(celeb => celeb.categoryValues.associate(categoryValueA))
    celebsTaggedWithB.map(celeb => celeb.categoryValues.associate(categoryValueB))
  
    val retrievedCategoryValuesA = instanceUnderTest.findByCategoryValueId(categoryValueA.id)
    val retrievedCategoryValuesB = instanceUnderTest.findByCategoryValueId(categoryValueB.id)
    
    // Returns the set associated with the filter
    retrievedCategoryValuesA.map(celeb => celebsTaggedWithA should contain(celeb))
    
    retrievedCategoryValuesB.map(celeb => celebsTaggedWithB should contain(celeb))

    // And the set is exclusive of the other filter
    retrievedCategoryValuesA.map(celeb => celebsTaggedWithB.exists(c => celeb.id == c.id) should be(false))
    retrievedCategoryValuesB.map(celeb => celebsTaggedWithA.exists(c => celeb.id == c.id) should be(false))    
  }

  // Private members
  //
  private def getPublishedCelebritiesWithIds(ids: Seq[Long]): IndexedSeq[Celebrity] = {
    instanceUnderTest.getPublishedCelebrities.toIndexedSeq.filter(celeb => ids.contains(celeb.id))
  }

  private def newSearchableCeleb : Celebrity = {
    val (customer, customer1, celebrity, product) = TestData.newSavedOrderStack()
    product.withPublishedStatus(PublishedStatus.Published).save()
    celebrity.withPublishedStatus(PublishedStatus.Published).withEnrollmentStatus(EnrollmentStatus.Enrolled).save()
  }

  private def instanceUnderTest: CelebrityStore = {
    AppConfig.instance[CelebrityStore]
  }
}
