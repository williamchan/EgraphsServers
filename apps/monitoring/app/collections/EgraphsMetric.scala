package collections

import play.api.libs.json.{Json, Writes, JsValue}

/**
 * We currently assume that any EgraphsMetric contains:
 *  	- A name
 * 		- A description
 * 		- A sequence of values
 * 
 * Use these 3 fields to contain the information necessary for your particular
 * kind of metric. For example, the website task sets description as the URL,
 * whereas other tasks can use it to store any additional necessary information,
 * or leave it blank.
 */
case class EgraphsMetric[A](name: String, description: String, values: IndexedSeq[A]) {

  def toJson(implicit typeWriter:Writes[Seq[A]]): JsValue = {
    Json.toJson(Map(
      "name" -> Json.toJson(name),
      "description" -> Json.toJson(description),
      "values" -> Json.toJson(values: Seq[A])))
  }
}