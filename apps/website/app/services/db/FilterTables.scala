package services.db

import org.squeryl.dsl.ast.LogicalBoolean

/**
 * Base definition of an object that can apply a Squeryl filter against a single
 * Table[T].
 *
 * @tparam T the type against whose Table[T] we would like to apply the filter.
 */
trait FilterOneTable[T] {
  /**
   * Applies the filter against a row from the table. For example, to include
   * only Celebrities with public name Shaq:
   *
   * {{{
   * override def test(row: celebrity): LogicalBoolean = {
   *   import org.squeryl.PrimitiveTypeMode._
   *
   *   row.publicName === "Shaq"
   * }
   * }}}
   */
  def test(row: T): LogicalBoolean
}

object FilterOneTable {
  /**
   * Reduces a list of one-table filters down into a single LogicalBoolean that applies
   * them all.
   */
  def reduceFilters[T](filters: Iterable[FilterOneTable[T]], row: T): LogicalBoolean = {
    import org.squeryl.PrimitiveTypeMode._

    filters.headOption match {
      case None =>
        (1 === 1)

      case Some(firstFilter) =>
        filters.tail.foldLeft(firstFilter.test(row))(
          (compositeFilter, nextFilter) =>
            (nextFilter.test(row) and compositeFilter)
        )
    }
  }
}


trait FilterTwoTables[T1, T2] extends FilterTables[T1, T2, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing] {
  def test(t1: T1, t2: T2): LogicalBoolean

  override final def test(t1: T1,
                          t2: Option[T2],
                          t3: Option[Nothing],
                          t4: Option[Nothing],
                          t5: Option[Nothing],
                          t6: Option[Nothing],
                          t7: Option[Nothing],
                          t8: Option[Nothing],
                          t9: Option[Nothing],
                          t10: Option[Nothing]): LogicalBoolean =
  {
    test(t1, t2.get)
  }
}

object FilterTwoTables {
  def reduceFilters[T1, T2](filters: Iterable[FilterTwoTables[T1, T2]], t1: T1, t2: T2): LogicalBoolean = {
    FilterTables.reduceFilters(filters, t1, Some(t2))
  }
}


trait FilterThreeTables[T1, T2, T3] extends FilterTables[T1, T2, T3, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing] {
  def test(t1: T1, t2: T2, t3: T3): LogicalBoolean

  override final def test(t1: T1,
                          t2: Option[T2],
                          t3: Option[T3],
                          t4: Option[Nothing],
                          t5: Option[Nothing],
                          t6: Option[Nothing],
                          t7: Option[Nothing],
                          t8: Option[Nothing],
                          t9: Option[Nothing],
                          t10: Option[Nothing]): LogicalBoolean =
  {
    test(t1, t2.get, t3.get)
  }
}

object FilterThreeTables {
  def reduceFilters[T, U, V](filters: Iterable[FilterThreeTables[T, U, V]], t1: T, t2: U, t3: V) = {
    FilterTables.reduceFilters(filters, t1, Some(t2), Some(t3))
  }
}

trait FilterTables[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10] {
  def test(t1: T1,
           t2: Option[T2],
           t3: Option[T3],
           t4: Option[T4],
           t5: Option[T5],
           t6: Option[T6],
           t7: Option[T7],
           t8: Option[T8],
           t9: Option[T9],
           t10: Option[T10]): LogicalBoolean
}

object FilterTables {
  def reduceFilters[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10] (
    filters: Iterable[FilterTables[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10]],
    t1: T1,
    t2: Option[T2] = None,
    t3: Option[T3] = None,
    t4: Option[T4] = None,
    t5: Option[T5] = None,
    t6: Option[T6] = None,
    t7: Option[T7] = None,
    t8: Option[T8] = None,
    t9: Option[T9] = None,
    t10: Option[T10] = None) =
  {
    import org.squeryl.PrimitiveTypeMode._

    filters.headOption match {
      case None =>
        (1 === 1)

      case Some(firstFilter) =>
        filters.tail.foldLeft(firstFilter.test(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10))(
          (compositeFilter, nextFilter) =>
            (nextFilter.test(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10) and compositeFilter)
        )
    }
  }
}