package services.db

trait InsertAndUpdateHooks[T] {
  private[db] var preInsertTransforms = Vector.empty[T => T]
  private[db] var preUpdateTransforms = Vector.empty[T => T]

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
}
