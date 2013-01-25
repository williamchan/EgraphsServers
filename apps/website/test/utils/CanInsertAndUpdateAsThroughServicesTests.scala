package utils

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import services.db.{HasEntity, CanInsertAndUpdateAsThroughServices, KeyedCaseClass}


trait CanInsertAndUpdateAsThroughServicesWithLongKeyTests[
  ModelT <: CanInsertAndUpdateAsThroughServices[ModelT,  EntityT] with HasEntity[EntityT, Long],
  EntityT <: KeyedCaseClass[Long]
] extends CanInsertAndUpdateAsThroughServicesTests[ModelT, EntityT, Long] {
  this: FlatSpec with ShouldMatchers =>

  override def newIdValue: Long = 0L
  override def improbableIdValue: Long = java.lang.Integer.MAX_VALUE
}

trait CanInsertAndUpdateAsThroughServicesTests[
  ModelT <: CanInsertAndUpdateAsThroughServices[ModelT,  EntityT] with HasEntity[EntityT, KeyT],
  EntityT <: KeyedCaseClass[KeyT],
  KeyT
] {
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

  "A restored model instance" should "be equivalent to the originally saved model" in {
    val saved = saveModel(newModel)
    val maybeRestored = restoreModel(saved.id)
    val maybeRestoredEntity = maybeRestored.map(model => model._entity)

    maybeRestored should not be (None)
    Some(saved._entity) should be (maybeRestoredEntity)
  }

  "Restoring an unsaved model id" should "return None" in {
    restoreModel(improbableIdValue) should be (None)
  }

  "Restoring an updated model instance" should "yield the updated version, not the original" in {
    val saved = saveModel(newModel)
    val updated = saveModel(transformModel(saved))
    val updatedRestored = restoreModel(updated.id).get

    updated.id should be (saved.id)
    updated._entity should not be (saved._entity)
    updatedRestored._entity should be (updated._entity)
    updatedRestored._entity should not be (saved._entity)
  }
}
