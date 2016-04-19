package kindSir.actors.repoWorker

import akka.actor._
import dispatch.Defaults._
import dispatch._
import kindSir.models._
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.util.{Failure, Success}

class RepoWorker(project: Project, baseUrl: String, token: String) extends Actor with ActorLogging {

  def receive = {
    case _ => log.error("Unknown message received")
  }
}

object RepoWorker {
  def props(project: Project, baseUrl: String, token: String) = Props(classOf[RepoWorker], project, baseUrl, token)

  case object FinishedRepoProcessing

  case class Process(repoId: Int)

}
