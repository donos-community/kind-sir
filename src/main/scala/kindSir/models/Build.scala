package kindSir.models

import org.json4s._
import scala.util.Try

case class Build(id: Integer, status: String, commit: Commit)

object Build {
  implicit val formats = DefaultFormats

  def parseList(json: JArray): Try[List[Build]] = Try(json.camelizeKeys.extract[List[Build]])
  def parse(json: JValue): Try[Build] = Try(json.camelizeKeys.extract[Build])
}
