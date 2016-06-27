package kindSir.models

import org.json4s._

import scala.util.Try

case class ProjectConf(upvotesThreshold: Int, vetoEnabled: Boolean, ignoreBuildStatus: Option[Boolean])

object ProjectConf {
  implicit val formats = DefaultFormats

  def parse(json: JValue): Try[ProjectConf] = Try(json.camelizeKeys.extract[ProjectConf])
}