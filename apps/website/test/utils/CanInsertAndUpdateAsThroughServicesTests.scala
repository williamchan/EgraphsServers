package utils

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import services.db.{HasEntity, CanInsertAndUpdateAsThroughServices, KeyedCaseClass}


trait CanInsertAndUpdateAsThroughServicesTests[
  ModelT <: CanInsertAndUpdateAsThroughServices[ModelT,  EntityT] with HasEntity[EntityT],
  EntityT <: KeyedCaseClass[Long]
] /*extends SavingEntityIdLongTests[EntityT]*/ {
  this: FlatSpec with ShouldMatchers =>

  private implicit def modelToIdDsl(model: ModelT) = IdDSL(model)
  private case class IdDSL(model: ModelT) { def id: Long = model._entity.id }

  // TODO(SER-499): these might be a bit strict for the statefulness of purchase flow types, either adapt to test consistency in state or remove state from purchase flow types

  def newIdValue: Long
  def improbableIdValue: Long
  def newModel: ModelT
  def saveModel(toSave: ModelT): ModelT
  def restoreModel(id: Long): Option[ModelT]

  // TODO(SER-499): which of these do we need?
  def transformModel(toTransform: ModelT): ModelT


  //
  // Test cases
  //
  "A new model instance" should "have id equal to " + newIdValue in {
    newModel.id should be === (newIdValue)
  }

  "A restore model instance" should "be equivalent to the originally saved on" in {
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

    updated should not be (inserted)
    updatedRestored should be (updated)
    updatedRestored should not be (inserted)
  }

}
