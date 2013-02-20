package models

import enums.{HasCallToActionTypeTests, HasPublishedStatusTests}
import utils._
import services.AppConfig

class MastheadTests extends EgraphsUnitTest
  with SavingEntityIdLongTests[Masthead]
  with CreatedUpdatedEntityTests[Long, Masthead]
  with DateShouldMatchers
  with DBTransactionPerTest
  with LandingPageImageTests[Masthead]
  with HasPublishedStatusTests[Masthead]
  with HasCallToActionTypeTests[Masthead]
{
  def store = AppConfig.instance[MastheadStore]


  "Masthead" should "require a headline" in new EgraphsTestApplication {
    val exception = intercept[IllegalArgumentException] {
      Masthead().save()
    }
    exception.getLocalizedMessage should include("Mastheads need headlines to be considered valid")
  }

  it should "return associated category values" in new EgraphsTestApplication {
    val masthead = Masthead(headline = TestData.generateUsername()).save()
    val category1 = TestData.newSavedCategory
    val categoryValue1 = TestData.newSavedCategoryValue(category1.id)

    masthead.categoryValues.associate(categoryValue1)

    masthead.categoryValues.size should be (1)
    masthead.categoryValues.exists(cv => cv.id == categoryValue1.id) should be (true)
  }
  //
  //  HasPublishedStatusTests[Masthead] methods
  //
  override def newPublishableEntity = {
      Masthead(name = TestData.generateFullname(), headline = TestData.generateUsername())
  }

  //
  // HasCallToActionTypeTests[Masthead]
  //

  override def newEntityWithCallToAction = {
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
