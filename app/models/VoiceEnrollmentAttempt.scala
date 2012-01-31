//package models
//
//import org.squeryl.PrimitiveTypeMode._
//import java.sql.Timestamp
//import services.Time
//import db.{KeyedCaseClass, Schema, Saves}
//
//case class VoiceEnrollmentAttempt(id: Long = 0,
//                                  celebrityId: Long,
//                                  vbgTransactionId: Long = 0L,
//                                  vbgStatus: Long = 0L,
//                                  created: Timestamp = Time.defaultTimestamp,
//                                  updated: Timestamp = Time.defaultTimestamp)
//  extends KeyedCaseClass[Long]
//  with HasCreatedUpdated {
//
//  //
//  // Public members
//  //
//  /**Persists by conveniently delegating to companion object's save method. */
//  def save(): VoiceEnrollmentAttempt = {
//    VoiceEnrollmentAttempt.save(this)
//  }
//
//  def attempt(): VoiceEnrollmentAttempt = {
//    // Find VoiceSamples and call VBG server
//    this
//  }
//
//  //
//  // KeyedCaseClass[Long] methods
//  //
//  override def unapplied = VoiceEnrollmentAttempt.unapply(this)
//
//}
//
//object VoiceEnrollmentAttempt extends Saves[VoiceEnrollmentAttempt] with SavesCreatedUpdated[VoiceEnrollmentAttempt] {
//
//  //
//  // Saves[VoiceEnrollmentAttempt] methods
//  //
//  override val table = Schema.voiceEnrollmentAttempts
//
//  override def defineUpdate(theOld: VoiceEnrollmentAttempt, theNew: VoiceEnrollmentAttempt) = {
//    updateIs(
//      theOld.celebrityId := theNew.celebrityId,
//      theOld.vbgTransactionId := theNew.vbgTransactionId,
//      theOld.vbgStatus := theNew.vbgStatus,
//      theOld.created := theNew.created,
//      theOld.updated := theNew.updated
//    )
//  }
//
//  //
//  // SavesCreatedUpdated[VoiceEnrollmentAttempt] methods
//  //
//  override def withCreatedUpdated(toUpdate: VoiceEnrollmentAttempt, created: Timestamp, updated: Timestamp) = {
//    toUpdate.copy(created = created, updated = updated)
//  }
//}