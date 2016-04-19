package kindSir.models

import org.json4s._
//import org.json4s.jackson.JsonMethods._

import scala.util.Try

/**
  * Created by nsa, 17/04/16 
  */

case class Group(id: Int, name: String, path: String, description: String, projects: List[Project])

object Group {
  implicit val formats = DefaultFormats

  def parseList(json: JArray): Try[List[Group]] = Try(json.camelizeKeys.extract[List[Group]])

  def parse(json: JValue): Try[Group] = Try(json.camelizeKeys.extract[Group])
}
