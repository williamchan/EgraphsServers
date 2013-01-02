package utils

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import services.db.{HasEntity, CanInsertAndUpdateAsThroughServices, KeyedCaseClass}


trait CanInsertAndUpdateAsThroughServicesTests[
  ModelT <: CanInsertAndUpdateAsThroughServices[ModelT,  EntityT] with HasEntity[EntityT, KeyT],
  EntityT <: KeyedCaseClass[KeyT],
  KeyT
] /*extends SavingEntityIdLongTests[EntityT]*/ {
  this: FlatSpec with ShouldMatchers =>

  def newIdValue: KeyT
  def improbableIdValue: KeyT
  def newModel: ModelT
  def saveModel(toSave: ModelT): ModelT
  def restoreModel(id: KeyT): Option[ModelT]
  def transformModel(toTransform: ModelT): ModelT


  //
  // Test cases
  //
  "A new model instance" should "have id equal to " + newIdValue in {
    newModel.id should be === (newIdValue)
  }

  "A restore model instance" should "be equivalent to the originally saved model" in {
    val saved = saveModel(newModel)
    val maybeRestored = restoreModel(saved.id)

    maybeRestored should not be (None)
    maybeRestored.get should be (saved)
  }

  "Restoring an unsaved model id" should "return None" in {
    restoreModel(improbableIdValue) should be (None)
  }

  "Restoring an updated model instance" should "yield the updated version, not the original" in {
    val inserted = saveModel(newModel)
    val updated = saveModel(transformModel(inserted))
    val updatedRestored = restoreModel(updated.id).get

    updated.id should be (inserted.id)
    updated should not be (inserted)
    updatedRestored should be (updated)
    updatedRestored should not be (inserted)
  }

}
