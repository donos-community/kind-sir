package kindSir.actors

import akka.actor.{Props, ActorLogging, Actor}
import com.typesafe.config.{ConfigFactory, Config}
import scala.collection.JavaConversions._

/**
  * Actor who reading Configuration for groups
  * and managing repo workers
  */
class GroupMonitor extends Actor with ActorLogging {

  import GroupMonitor._

  private var url: Option[String] = None
  private var token: Option[String] = None
  private var groups: Option[List[String]] = None

  def receive = {
    case ReloadConfig => reloadConfig()
    case _ => System.out.println("Received message, don't giving a shit.")
  }

  def reloadConfig() = {
    val conf = ConfigFactory.load().getConfig("kindSir")
    this.url = Option(conf.getString("gitlab-url"))
    this.token = Option(conf.getString("gitlab-token"))
    this.groups = Option(conf.getStringList("groups").toList)

    log.info("URL: {}", this.url)
    log.info("token: {}", this.token)
    log.info("groups: {}", this.groups)
  }
}

object GroupMonitor {
  case object ReloadConfig
  val props = Props[GroupMonitor]
}