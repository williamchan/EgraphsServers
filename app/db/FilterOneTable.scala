package db

import org.squeryl.dsl.ast.LogicalBoolean

/**
 * Base definition of an object that can apply a Squeryl filter against a single
 * Table[T].
 *
 * @param T the type against whose Table[T] we would like to apply the filter.
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
