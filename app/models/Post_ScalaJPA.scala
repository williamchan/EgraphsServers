package models

import javax.persistence.Entity
import play.data.validation.Required
import play.db.jpa.Model

@Entity
class Post_ScalaJPA extends Model {

  @Required
  var title: String = null

  @Required
  var body: String = null
}
