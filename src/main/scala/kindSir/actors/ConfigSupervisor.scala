package kindSir.actors

import akka.actor.{ActorRef, Actor, ActorLogging, Props}
import kindSir.models._



/**
  * Actor who reading Configuration for groups
  * and managing repo workers
  */
class ConfigSupervisor extends Actor with ActorLogging {

  var config: Option[AppConfig] = None
  var groupSupervisors: List[ActorRef] = List()

  reloadConfig()
  startGroupSupervisors()

  def receive = {
    case _ => log.info("Received unknown, don't giving a shit.")
  }

  def reloadConfig() = AppConfig.reload() match {
    case scala.util.Success(conf) =>
      this.config = Some(conf)
      log.info("Config loaded: {}", this.config)
    case scala.util.Failure(exc) =>
      log.error("Error reading config: {}", exc)
      throw exc
  }

  def startGroupSupervisors() = this.config match {
    case Some(conf) =>
      this.groupSupervisors = conf.groups map { g =>
        context.actorOf(GroupSupervisor.props(g, conf.baseUrl, conf.token), g.name)
      }
    case None =>
      log.info("No config loaded")
  }
}

object ConfigSupervisor {
  val props = Props[ConfigSupervisor]
}