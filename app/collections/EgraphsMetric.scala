package collections

import play.api.libs.json.{Json, Writes, JsValue}

case class EgraphsMetric[A](name: String, description: String, values: IndexedSeq[A]) {

  def toJson(implicit typeWriter:Writes[Seq[A]]): JsValue = {
    Json.toJson(Map(
      "name" -> Json.toJson(name),
      "description" -> Json.toJson(description),
      "values" -> Json.toJson(values: Seq[A])))
  }
}