package kindSir.models

import org.json4s.{DefaultFormats, JValue}
import scala.util.Try

case class MergeRequest(id: Int, projectId: Int, upvotes: Int, downvotes: Int)

object MergeRequest {

  implicit val formats = DefaultFormats

  def parse(json: JValue): Try[MergeRequest] = Try(json.camelizeKeys.extract[MergeRequest])
}