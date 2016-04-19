package kindSir.models

import org.json4s._
import org.json4s.JsonAST.JValue

import scala.util.Try

case class File(id: String, name: String, elementType: String)

object File {

  implicit val formats = DefaultFormats

  private def transformJson(json: JValue): JValue = json.camelizeKeys.transformField { case ("type", t) => ("elementType", t) }

  def parseList(json: JArray): Try[List[File]] = {
    val transformedJson = this.transformJson(json)
    Try(transformedJson.camelizeKeys.extract[List[File]])
  }

  def parse(json: JValue): Try[File] = {
    val transformedJson = this.transformJson(json)
    Try(transformedJson.extract[File])
  }
}
