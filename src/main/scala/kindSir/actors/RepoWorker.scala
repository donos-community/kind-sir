package kindSir.actors

import akka.actor._
import kindSir.actors.RepoWorker.{ProcessRequests, SetConfig, Stop}
import kindSir.gitlab.GitlabAPI
import kindSir.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class RepoWorker(project: Project, gitlab: GitlabAPI) extends Actor with ActorLogging {

  fetchConfig()

  def receive = {
    case ProcessRequests(requests) =>
      log.debug(s"Processing requests: $requests")
      acceptRequests(requests)
    case Stop(reason) =>
      log.info(s"Stopping because of: $reason")
      context.stop(self)
    case SetConfig(conf) =>
      log.debug(s"Config set to: $conf")
      fetchMergeRequests(conf)
    case msg =>
      log.error(s"Unknown message received: $msg")
  }

  def fetchConfig() = {
    val actor = self
    gitlab.projectConfig(project) onComplete {
      case Success(conf) => actor ! SetConfig(conf)
      case Failure(exc) => actor ! Stop(s"No Config found. Ignoring ${project.name}")
    }
  }

  def fetchMergeRequests(conf: ProjectConf) = {
    val actor = self
    gitlab.fetchMergeRequests(project) onComplete {
      case Success(requests) =>
        val readyForMerge = requests.filter { req =>
          req.upvotes >= conf.upvotesThreshold && (if (conf.vetoEnabled) req.downvotes == 0 else true)
        }
        if (readyForMerge.nonEmpty) {
          actor ! ProcessRequests(readyForMerge)
        } else {
          actor ! Stop(s"No requests to be processed for ${project.name}")
        }
      case Failure(exc) =>
        throw exc
    }
  }

  def acceptRequests(requests: List[MergeRequest]) = {
    val actor = self
    gitlab.acceptMergeRequests(requests) onComplete {
      case Success(list) =>
        log.debug(s"Result of merge accept: $list")
        actor ! Stop("Everything merged")
      case Failure(exc) =>
        log.error(s"Merge failed with exception: $exc")
        actor ! Stop(s"Unable to merge request for project: ${project.name}")
    }
  }
}

object RepoWorker {
  def props(project: Project, gitlab: GitlabAPI) = Props(classOf[RepoWorker], project, gitlab)

  case class SetConfig(conf: ProjectConf)

  case class ProcessRequests(requests: List[MergeRequest])

  case class Stop(reason: String)

}
