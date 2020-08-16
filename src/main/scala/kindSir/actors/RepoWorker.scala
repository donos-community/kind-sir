package kindSir.actors

import akka.actor._
import kindSir.actors.RepoWorker._
import kindSir.gitlab.GitlabAPI
import kindSir.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Repo Worker actor.
  *
  * @param project Project object to process
  * @param gitlab  Gitlab API instance
  */
class RepoWorker(project: Project, gitlab: GitlabAPI) extends Actor with ActorLogging {

  fetchConfig()

  private var config: Option[ProjectConf] = None
  private var latestBuilds: Option[List[Build]] = None

  def receive = {

    case SetConfig(conf) =>
      config = Some(conf)
      val system = ActorSystem("KindSir")
      system.scheduler.scheduleWithFixedDelay(0.seconds, 60.seconds) { () =>
        log.info(s"Processing ${project.name}")
        if (conf.ignoreBuildStatus.getOrElse(false))
          fetchMergeRequests(conf)
        else
          fetchBuilds(project)
      }

    case SetBuilds(builds) =>
      latestBuilds = Some(builds)
      fetchMergeRequests(config.get)

    case ProcessRequests(requests) =>
      processRequests(requests)

    case AcceptRequests(requests) =>
      acceptRequests(requests)

    case Stop(reason) =>
      if (reason.startsWith("No Config found")) {
        log.info(s"Stopping because of: $reason")
        context.stop(self)
      }

    case msg =>
      log.error(s"Unknown message received: $msg")
  }

  def fetchBuilds(project: Project) = {
    val actor = self
    gitlab.fetchLatestBuilds(project.id) onComplete {
      case Success(builds) => actor ! SetBuilds(builds)
      case Failure(exc) => actor ! Stop(s"Problem occurred while fetching builds: $exc")
    }
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
        val notInProgress = requests.filterNot(req => req.workInProgress)
        val upvoted = notInProgress.filter { req =>
          (req.upvotes - req.downvotes) >= conf.upvotesThreshold && (if (conf.vetoEnabled) req.downvotes == 0 else true)
        }
        if (upvoted.nonEmpty)
          actor ! ProcessRequests(upvoted)
        else
          actor ! Stop(s"No requests to be processed for ${project.name}")

      case Failure(exc) => throw exc
    }
  }

  def processRequests(mergeRequests: List[MergeRequest]) = {
    val actor = self
    val readyForMerge = mergeRequests.filter { req =>
      if (config.get.ignoreBuildStatus.getOrElse(false)) {
        true
      }
      else {
        val lastCommit = Await.result(gitlab.fetchCommitsFor(req), 15.seconds).head
        val successfulBuilds = this.latestBuilds.getOrElse(List()).filter(_.commit.shortId == lastCommit.shortId)
        successfulBuilds.nonEmpty
      }
    }
    if (readyForMerge.nonEmpty)
      actor ! AcceptRequests(readyForMerge)
    else
      actor ! Stop(s"No request to be merged for ${project.name}")
  }

  def acceptRequests(requests: List[MergeRequest]) = {
    val actor = self
    gitlab.acceptMergeRequests(requests) onComplete {
      case Success(list) =>
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

  case class SetBuilds(builds: List[Build])

  case class SetRequests(requests: List[MergeRequest])

  case class ProcessRequests(requests: List[MergeRequest])

  case class AcceptRequests(requests: List[MergeRequest])

  case class Stop(reason: String)
}
