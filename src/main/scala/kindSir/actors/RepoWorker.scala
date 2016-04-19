package kindSir.actors

import akka.actor._
import kindSir.actors.RepoWorker.{SetConfig, StopWithReason}
import kindSir.gitlab.GitlabAPI
import kindSir.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class RepoWorker(project: Project, gitlab: GitlabAPI) extends Actor with ActorLogging {

  fetchConfig()

  var config: Option[ProjectConf] = None

  def receive = {
    case StopWithReason(reason) =>
      log.info(s"Stopping because of: $reason")
      context.stop(self)
    case SetConfig(conf) =>
      log.info(s"Config set to: $conf")
      this.config = Some(conf)
    case _ =>
      log.error("Unknown message received")
  }

  def fetchConfig() = {
    val actor = self
    gitlab.projectConfig(project) onComplete {
      case Success(conf) => actor ! SetConfig(conf)
      case Failure(exc) => actor ! StopWithReason(s"No Config found. Ignoring ${project.name}")
    }
  }
}

object RepoWorker {
  def props(project: Project, gitlab: GitlabAPI) = Props(classOf[RepoWorker], project, gitlab)

  case class SetConfig(conf: ProjectConf)

  case class StopWithReason(reson: String)

  case class Process(repoId: Int)

}
