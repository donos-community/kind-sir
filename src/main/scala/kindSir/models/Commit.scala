package kindSir.models

import org.json4s._
import scala.util.Try

case class Commit(id: String, shortId: String)

object Commit {
  implicit val formats = DefaultFormats

  def parseList(json: JArray): Try[List[Commit]] = Try(json.camelizeKeys.extract[List[Commit]])
  def parse(json: JValue): Try[Commit] = Try(json.camelizeKeys.extract[Commit])
}
