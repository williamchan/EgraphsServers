package models.frontend.admin

import models.Celebrity

case class CelebrityAdminViewModel(
  id: Long,
  publicName: String,
  isFeatured: Boolean)