package kindSir.actors

import akka.actor._
import kindSir.models._
import dispatch._
import Defaults._
import kindSir.actors.repoWorker.RepoWorker
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.util.{Failure, Success}
import scala.concurrent.duration._


class GroupSupervisor(groupConfig: GroupConfig, baseUrl: String, token: String) extends Actor with ActorLogging {

  import GroupSupervisor._

  var group: Option[Group] = None
  var repoWorkers: List[ActorRef] = List()

  fetchGroupId(groupConfig.name)

  def receive = {
    case Start =>
      val g = this.group.get
      this.repoWorkers = g.projects map { p =>
        val child = context.actorOf(RepoWorker.props(p, baseUrl, token))
        context.watch(child)
        child
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
    val groupsUrl = url(s"$baseUrl/api/v3/groups/${groupConfig.name}?private_token=$token")
    val capturedSelf = self
    Http(groupsUrl OK as.String) onComplete {
      case Success(string) => capturedSelf ! SetGroup(Group.parse(parse(string)).get)
      case Failure(error) => throw error
    }
  }
}


object GroupSupervisor {

  case object Start

  case class SetGroup(group: Group)

  def props(group: GroupConfig, baseUrl: String, token: String) =
    Props(classOf[GroupSupervisor], group, baseUrl, token)
}