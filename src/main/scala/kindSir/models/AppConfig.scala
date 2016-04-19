package kindSir.models

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConversions._
import scala.util.Try

case class GroupConfig(name: String)

case class AppConfig(baseUrl: String, token: String, groups: List[GroupConfig])

object AppConfig {

  def reload(): Try[AppConfig] = {
    ConfigFactory.invalidateCaches()
    val conf = ConfigFactory.load().getConfig("kindSir")
    val baseUrl = Option(conf.getString("gitlab-url"))
    val token = Option(conf.getString("gitlab-token"))
    val groups = Option(conf.getStringList("groups").toList)

    val config = for {
      url <- baseUrl
      t <- token
      gs <- groups
    } yield AppConfig(url, t, gs.map(GroupConfig(_)))

    Try(config.get)
  }
}
