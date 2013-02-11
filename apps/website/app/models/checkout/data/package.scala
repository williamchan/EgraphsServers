package models.checkout

import play.api.libs.json.Json
import play.api.data.{Form, Forms, FormError, Mapping}

package object data {

  sealed abstract class ApiFormError(val name: String)

  object ApiFormError extends egraphs.playutils.Enum {
    sealed abstract class EnumVal(name: String) extends ApiFormError(name) with Value
    val InvalidLength = new EnumVal("invalid_length") {}
    val InvalidFormat = new EnumVal("invalid_format") {}
    val InvalidType = new EnumVal("unexpected_type") {}
    val Required = new EnumVal("required") {}
  }

  val fieldKey = "field"
  val causeKey = "cause"
  val errorsKey = "errors"




  /** makes an individual json error object */
  def createJsonErrors(errors: Seq[(String, String)]) = Json.toJson {
    def createJsonErrorValueObject(error: (String, String)) = {
      def field(fieldName: String) = (fieldKey -> Json.toJson(fieldName))
      def cause(causeValue: String) = (causeKey -> Json.toJson(causeValue))

      Json.toJson{ Map( field(error._1), cause(error._2) ) }
    }

    Map( errorsKey -> errors.map{ createJsonErrorValueObject(_) } )
  }





}
