package models

import enums.HasPublishedStatusTests
import utils._
import services.AppConfig

class MastheadTests extends EgraphsUnitTest
  with SavingEntityIdLongTests[Masthead]
  with CreatedUpdatedEntityTests[Long, Masthead]
  with DateShouldMatchers
  with DBTransactionPerTest
  with LandingPageImageTests[Masthead]
  with HasPublishedStatusTests[Masthead]
{
  def store = AppConfig.instance[MastheadStore]


  "Masthead" should "require a headline" in new EgraphsTestApplication {
    val exception = intercept[IllegalArgumentException] {
      Masthead().save()
    }
    exception.getLocalizedMessage should include("Mastheads need headlines to be considered valid")
  }
  //
  //  HasPublishedStatus[Masthead] methods
  //
  override def newPublishableEntity = {
    Masthead(name = TestData.generateFullname(), headline = TestData.generateUsername())
  }

  //
  // LandingPageImageTests[Masthead] methods
  //
  
  override def newEntityWithLandingPageImage = {
    Masthead(name = TestData.generateFullname(), headline = TestData.generateUsername())
  }


  // SavingEntityTests[Account] methods
  //
  override def newEntity = {
    Masthead(name = TestData.generateUsername(), headline = TestData.generateUsername())
  }

  override def saveEntity(toSave: Masthead) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: Masthead) = {
    toTransform.copy(
      name = "name",
      headline= "headline",
      subtitle = Option("something")
    )
  }

}
