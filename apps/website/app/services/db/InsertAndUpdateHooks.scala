package services.db

/**
 * Provides external hooks for attaching events to insert and update of
 * a persistable entity type. Most usually used through mixing the
 * [[services.db.InsertsAndUpdates]] subtype.
 *
 * Usage:
 * {{{
 *   case class Person(id: Long = 0L, name: String = "", created: Date = new Date(0L)) extends KeyedEntity[Long]
 *
 *   object Person extends InsertsAndUpdates[Person] {
 *     // After this command, any insertion of a Person through this object will also
 *     // set the Created date.
 *     beforeInsert(personToTransform => personToTransform.copy(created=new Date()))
 *
 *     override val table: Table[Person] = MySquerylDb.people
 *
 *     def testHook {
 *       val inserted = insert(Person())
 *       assert(inserted.created.getTime != 0L)
 *     }
 *   }
 * }}}
 *
 * @tparam T the entity type being persisted
 */
trait InsertAndUpdateHooks[T] {
  private[db] var preInsertTransforms = Vector.empty[T => T]
  private[db] var preUpdateTransforms = Vector.empty[T => T]

  /**
   * Transforms the provided instance with all registered insert hooks, returning
   * the transformed copy.
   */
  protected def performPreInsertHooks(toInsert: T): T = {
    performTransforms(preInsertTransforms, toInsert)
  }

  /**
   * Transforms the provided instance with all registered update hooks, returning
   * the transformed copy.
   */
  protected def performPreUpdateHooks(toUpdate: T): T = {
    performTransforms(preUpdateTransforms, toUpdate)
  }

  /**
   * Hook to provide an entity transform that will be applied before inserting any
   * new object.
   *
   * See class documentation for usage.
   */
  final def beforeInsert(transform: (T) => T) {
    preInsertTransforms = preInsertTransforms ++ Vector(transform)
  }

  /**
   * Hook to provide a transform to apply before updating any new object.
   *
   * See class documentation for usage.
   */
  final def beforeUpdate(transform: (T) => T) {
    preUpdateTransforms = preUpdateTransforms ++ Vector(transform)
  }

  /**
   * Hook to provide an entity transform that will be applied before inserting or updating any
   * new object.
   *
   * See class documentation for usage.
   */
  final def beforeInsertOrUpdate(transform: (T) => T) {
    beforeInsert(transform)
    beforeUpdate(transform)
  }

  //
  // Private members
  //
  private def performTransforms(transforms: Seq[(T) => T], entityToTransform: T): T = {
    transforms.foldLeft(entityToTransform)((currEntity, nextTransform) => nextTransform(currEntity))
  }
}
