package models.frontend

case class PaginationInfo(
  showPaging: Boolean,
  totalResultsStr: String,
  firstUrl: Option[String],
  prevUrl: Option[String],
  nextUrl: Option[String],
  lastUrl: Option[String]
)