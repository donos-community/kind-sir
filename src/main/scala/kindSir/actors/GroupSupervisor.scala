package kindSir.actors

import akka.actor._
import dispatch.Defaults._
import kindSir.gitlab._
import kindSir.models._

import scala.concurrent.duration._
import scala.util.{Failure, Success}


class GroupSupervisor(groupConfig: GroupConfig, gitlab: GitlabAPI) extends Actor with ActorLogging {

  import GroupSupervisor._

  var group: Option[Group] = None

  fetchGroup(groupConfig.name)

  def receive = {
    case Start => this.group.get.projects.foreach { p =>
      val child = context.actorOf(RepoWorker.props(p, gitlab))
      context.watch(child)
    }
    case SetGroup(g) =>
      this.group = Some(g)
      log.debug(s"Group set to: ${this.group}")
      val actor = self
      context.system.scheduler.schedule(0.seconds, 60.seconds) {
        actor ! Start
      }
    case Terminated(child) =>
      log.debug(s"Child $child was terminated")
    case msg =>
      log.error(s"Unknown message $msg")
  }

  def fetchGroup(name: String) = {
    val actor = self
    gitlab.group(groupConfig.name) onComplete {
      case Success(g) => actor ! SetGroup(g)
      case Failure(error) => throw error
    }
  }
}


object GroupSupervisor {

  case object Start

  case class SetGroup(group: Group)

  def props(group: GroupConfig, gitlab: GitlabAPI) = Props(classOf[GroupSupervisor], group, gitlab)
}