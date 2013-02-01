package utils

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import services.db._
import scala.Some
import services.cache.Cache


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





trait HasTransientServicesTests[ModelT] {
  this: FlatSpec with ShouldMatchers =>


  def newModel: ModelT

  def modelsEqual(a: ModelT, b: ModelT): Boolean
  def cacheInstance: Cache




  "A model with transient services" should "serialize and deserialize as expected" in {
    val model = newModel
    writeModel(model)
    val deserialized = readModel()

    assert(modelsEqual(model, deserialized), "Deserialized model did not have expected value.")
  }

  it should "be cacheable" in {
    val model = newModel
    val key = TestData.random.nextString(10)

    cacheInstance.set(key, model, 5)

    val fromCache = cacheInstance.get(key)

    fromCache should be ('defined)
    assert(modelsEqual(model, fromCache.get), "Model from cache did not have expected value.")
  }


  import java.io._
  protected val objBuffer = new ByteArrayOutputStream()
  protected def writeModel(model: ModelT) = {
    objBuffer.reset()
    val objWriter = new ObjectOutputStream(objBuffer);
    objWriter.writeObject(model)
  }

  protected def readModel() = {
    val objReader = new ObjectInputStream( new ByteArrayInputStream(objBuffer.toByteArray) )
    objReader.readObject.asInstanceOf[ModelT]
  }

}