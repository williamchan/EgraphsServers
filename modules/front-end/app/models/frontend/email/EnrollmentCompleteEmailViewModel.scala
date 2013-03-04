package models.frontend.email

import java.sql.Timestamp

case class EnrollmentCompleteEmailViewModel(
  celebrityName: String,
  videoAssetUploaded: Boolean,
  celebrityEnrollmentStatus: String,
  timeEnrolled: Timestamp // this will be celebrity updated field
)