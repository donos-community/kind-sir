package kindSir.actors

import akka.actor._
import kindSir.gitlab.{Gitlab, GitlabAPI}
import kindSir.models._

import scala.util.{Failure, Success}

/**
  * Actor who reading Configuration for groups
  * and managing groups workers
  */
class ConfigSupervisor extends Actor with ActorLogging {

  var config: Option[AppConfig] = None
  var groupSupervisors: List[ActorRef] = List()
  var gitlab: Option[GitlabAPI] = None

  reloadConfig()
  startGroupSupervisors()

  def receive = {
    case _ => log.info("Received unknown, don't giving a shit.")
  }

  def reloadConfig() = {
    val conf = AppConfig.reload().get
    this.config = Some(conf)
    this.gitlab = Some(Gitlab(conf.baseUrl, conf.token))
  }

  def startGroupSupervisors() = this.groupSupervisors = this.config.get.groups map { g =>
    context.actorOf(GroupSupervisor.props(g, gitlab.get), g.name)
  }
}

object ConfigSupervisor {
  val props = Props[ConfigSupervisor]
}