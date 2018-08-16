package kindSir.models

import com.typesafe.config.ConfigFactory

import scala.util.Try

case class GroupConfig(name: String)

case class AppConfig(baseUrl: String, token: String, apiVersion: Int, groups: List[GroupConfig])

object AppConfig {

  def reload(): Try[AppConfig] = {
    ConfigFactory.invalidateCaches()
    val conf = ConfigFactory.load().getConfig("kindSir")
    val baseUrl = Option(conf.getString("gitlab-url"))
    val token = Option(conf.getString("gitlab-token"))
    val defaultAPIVersion = 4
    val apiVersion = Option(conf.getInt("gitlab-api-version")).getOrElse(defaultAPIVersion)

    val config = for {
      url <- baseUrl
      t <- token
    } yield AppConfig(url, t, apiVersion, List())

    Try(config.get)
  }
}
