package services

/**
 * Helper methods for serialization
 */
object Serialization {
  /**
   * Turns a set of (String -> Option) tuples into a map containing only the fields
   * whose Options contained some value.
   */
  def makeOptionalFieldMap(optionalFields: List[(String, Option[Any])]): Map[String, Any] = {
    optionalFields.foldLeft(Map.empty[String, Any])((growingMap, nextField) =>
      nextField._2 match {
        case None => growingMap
        case Some(value) => growingMap + (nextField._1 -> value)
      }
    )
  }
}