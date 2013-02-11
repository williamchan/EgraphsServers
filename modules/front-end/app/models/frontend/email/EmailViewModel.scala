package models.frontend.email

case class EmailViewModel(
  subject: String,
  fromEmail: String = "webserver@egraphs.com",
  fromName: String = "Egraphs",
  toAddresses: List[(String, Option[String])],
  bccAddress: Option[String] = None
)