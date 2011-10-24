package models

import javax.persistence.{InheritanceType, Inheritance, Entity}
import play.db.jpa.Model

@Entity
@Inheritance(strategy=InheritanceType.JOINED)
abstract class User extends Model with CreatedUpdated with PasswordProtected
