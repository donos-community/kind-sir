package kindSir.actors

import akka.actor._
import kindSir.gitlab.{Gitlab, GitlabAPI}
import kindSir.models._
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

class ConfigSupervisor extends Actor with ActorLogging {

  import ConfigSupervisor._

  var config: Option[AppConfig] = None
  var gitlab: Option[GitlabAPI] = None

  reloadConfig()
  fetchGroups()

  def receive = {
    case SetGroups(groups) =>
      this.config = Some(
        AppConfig(
          this.config.get.baseUrl,
          this.config.get.token,
          this.config.get.apiVersion,
          groups map { g => GroupConfig(g.path) }))
      startGroupSupervisors()
    case msg => log.error(s"Received unknown message: $msg")
  }

  def reloadConfig() = {
    this.config = AppConfig.reload().toOption
    this.gitlab = Some(Gitlab(this.config.get.baseUrl, this.config.get.token, this.config.get.apiVersion))
  }

  def fetchGroups() = {
    val actor = self
    this.gitlab.get.groups() onComplete {
      case Success(groups) =>
        log.debug("Set groups to: {}", groups)
        actor ! SetGroups(groups)
      case Failure(exc) =>
        log.error(s"No groups found for KindSir: $exc")
    }
  }

  def startGroupSupervisors() = this.config.get.groups map { g =>
    context.actorOf(GroupSupervisor.props(g, gitlab.get), g.name)
  }
}

object ConfigSupervisor {
  val props = Props[ConfigSupervisor]

  case class SetGroups(groups: List[Group])
}