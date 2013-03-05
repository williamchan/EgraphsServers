package models.frontend.email

import java.sql.Timestamp

case class EnrollmentCompleteEmailViewModel(
  celebrityName: String,
  videoAssetIsDefined: Boolean,
  celebrityEnrollmentStatus: String,
  timeEnrolled: String // this will be celebrity updated field
)