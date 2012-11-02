package models

import java.sql.Timestamp
import services.{ AppConfig, Time }
import services.db.{ FilterOneTable, KeyedCaseClass, Schema, SavesWithLongKey }

case class VideoAsset(id: Long = 0,
  created: Timestamp = Time.defaultTimestamp,
  updated: Timestamp = Time.defaultTimestamp,
  url: String) //extends KeyedCaseClass[Long]
  //with HasCreatedUpdated {
  {

}