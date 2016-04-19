package kindSir.models

import org.json4s.DefaultFormats
import org.json4s.JsonAST.JValue

import scala.util.Try

case class File(id: String, name: String, mode: Int, elementType: String)

object File {

  implicit val formats = DefaultFormats

  def parse(json: JValue): Try[File] = {
    val transformedJson = json.camelizeKeys.transformField { case ("type", t) => ("elementType", t) }
    Try(transformedJson.extract[File])
  }
}
