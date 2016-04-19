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
  var repoWorkers: List[ActorRef] = List()

  fetchGroupId(groupConfig.name)

  def receive = {
    case Start =>
      val g = this.group.get
      this.repoWorkers = g.projects map { p =>
        val child = context.actorOf(RepoWorker.props(p, gitlab))
        context.watch(child)
      }
    case SetGroup(g) =>
      this.group = Some(g)
      log.info(s"Group set to: ${this.group}")
      val capturedSelf = self
      context.system.scheduler.schedule(0.seconds, 60.seconds) {
        capturedSelf ! Start
      }
    case Terminated(child) =>
      log.info(s"Child $child was terminated")
    case msg =>
      log.info(s"Unknown message $msg")
  }

  def fetchGroupId(name: String) = {
    val capturedSelf = self
    gitlab.group(groupConfig.name) onComplete {
      case Success(g) => capturedSelf ! SetGroup(g)
      case Failure(error) => throw error
    }
  }
}


object GroupSupervisor {

  case object Start

  case class SetGroup(group: Group)

  def props(group: GroupConfig, gitlab: GitlabAPI) =
    Props(classOf[GroupSupervisor], group, gitlab)
}