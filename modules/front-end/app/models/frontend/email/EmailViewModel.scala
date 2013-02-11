package models.frontend.email

case class EmailViewModel(
  subject: String,
  fromEmail: String = "webserver@egraphs.com",
  fromName: String = "Egraphs",
  toEmail: String,
  bccAddress: Option[String] = None
)