package kindSir.actors

import akka.actor._
import kindSir.gitlab.{Gitlab, GitlabAPI}
import kindSir.models._

class ConfigSupervisor extends Actor with ActorLogging {

  var config: Option[AppConfig] = None
  var gitlab: Option[GitlabAPI] = None

  reloadConfig()
  startGroupSupervisors()

  def receive = {
    case msg => log.error(s"Received unknown message: $msg")
  }

  def reloadConfig() = {
    this.config = AppConfig.reload().toOption
    this.gitlab = Some(Gitlab(this.config.get.baseUrl, this.config.get.token))
  }

  def startGroupSupervisors() = this.config.get.groups map { g =>
    context.actorOf(GroupSupervisor.props(g, gitlab.get), g.name)
  }
}

object ConfigSupervisor {
  val props = Props[ConfigSupervisor]
}