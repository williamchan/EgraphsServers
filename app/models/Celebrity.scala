package models

import org.squeryl.PrimitiveTypeMode._
import libs.Time
import java.sql.Timestamp
import db.{KeyedCaseClass, Schema, Saves}
import sjson.json.{Format, DefaultProtocol}

/**
 * Persistent entity representing the Celebrities who provide products on
 * our service.
 */
case class Celebrity(
  id: Long = 0,
  apiKey: Option[String] = None,
  description: Option[String] = None,
  firstName: Option[String]   = None,
  lastName: Option[String]    = None,
  popularName: Option[String] = None,
  profilePhotoId: Option[String] = None,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp
) extends KeyedCaseClass[Long] with HasCreatedUpdated
{
  def save(): Celebrity = Celebrity.save(this)
  override def unapplied = Celebrity.unapply(this)

  def addOptionalFields(map: Map[String, Any], fields: List[(String, Option[Any])]) = {
    fields.foldLeft(map)((growingMap, nextField) =>
      nextField._2 match {
        case None => growingMap
        case Some(value) => growingMap + (nextField._1 -> value)
      }
    )
  }

  def renderedForApi: Map[String, Any] = {
    val map = Map("id" -> id)
    val optionalFields = List(
      ("firstName" -> firstName),
      ("lastName" -> lastName),
      ("popularName" -> popularName)
    )

    addOptionalFields(map, optionalFields)
  }
}

object Celebrity extends Saves[Celebrity] with SavesCreatedUpdated[Celebrity] {
  //
  // Public Methods
  //

  //
  // Saves[Celebrity] methods
  //
  override val table = Schema.celebrities

  override def defineUpdate(theOld: Celebrity, theNew: Celebrity) = {
    updateIs(
      theOld.apiKey := theNew.apiKey,
      theOld.description := theNew.description,
      theOld.firstName := theNew.firstName,
      theOld.lastName := theNew.lastName,
      theOld.popularName := theNew.popularName,
      theOld.profilePhotoId := theNew.profilePhotoId,
      theOld.created := theNew.created,
      theOld.updated := theNew.updated
    )
  }

  //
  // SavesCreatedUpdated[Celebrity] methods
  //
  override def withCreatedUpdated(toUpdate: Celebrity, created: Timestamp, updated: Timestamp) = {
    toUpdate.copy(created=created, updated=updated)
  }
}
