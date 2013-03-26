package models.frontend.email

case class EmailViewModel(
  subject: String,
  fromEmail: String = "webserver@egraphs.com",
  fromName: String = "Egraphs",
  toAddresses: List[(String, Option[String])],
  replyToEmail: String = "webserver@egraphs.com"
)