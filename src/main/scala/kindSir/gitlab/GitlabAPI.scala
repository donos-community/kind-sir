package kindSir.gitlab

import dispatch.Defaults._
import dispatch._
import kindSir.models._
import org.json4s._
import org.json4s.jackson.JsonMethods._

trait GitlabAPI {

  val baseUrl: String
  val token: String

  def groups(): Future[List[Group]] = {
    val all = url(s"$baseUrl/api/v3/groups/?private_token=$token")
    Http(all OK as.String) map { str =>
      parse(str) match {
        case list@JArray(_) => Group.parseList(list).get
        case _ => throw new RuntimeException("No groups found")
      }
    }
  }

  def group(groupName: String): Future[Group] = {
    val groupsUrl = url(s"$baseUrl/api/v3/groups/$groupName?private_token=$token")
    Http(groupsUrl OK as.String) map {string => Group.parse(parse(string)).get }
  }

  def projectConfig(project: Project): Future[ProjectConf] = {
    val treeUrl = url(s"$baseUrl/api/v3/projects/${project.id}/repository/tree?private_token=$token")
    Http(treeUrl OK as.String) map { string =>
      parse(string) match {
        case list@JArray(_) => File.parseList(list).get
        case _ => throw new RuntimeException("No projects found")
      }
    } map { files =>
      files.filter(_.name equalsIgnoreCase ".kind_sir.conf").head
    } flatMap { file =>
      val confUrl = url(s"$baseUrl/api/v3/projects/${project.id}/repository/raw_blobs/${file.id}?private_token=$token")
      Http(confUrl OK as.String) map { str => ProjectConf.parse(parse(str)).get }
    }
  }

  def fetchMergeRequests(project: Project): Future[List[MergeRequest]] = {
    val requestsUrl = url(s"$baseUrl/api/v3/projects/${project.id}/merge_requests?state=opened")
      .addHeader("PRIVATE-TOKEN", token)

    Http(requestsUrl OK as.String) map { str =>
      parse(str) match {
        case list@JArray(_) => MergeRequest.parseList(list).get
        case _ => throw new RuntimeException(s"No merge requests for project ${project.name} found")
      }
    }
  }

  def acceptMergeRequest(request: MergeRequest): Future[String] = {
    val acceptUrl = url(s"$baseUrl/api/v3/projects/${request.projectId}/merge_request/${request.id}/merge")
        .addHeader("PRIVATE-TOKEN", token)
        .PUT
    Http(acceptUrl OK as.String)
  }

  def acceptMergeRequests(requests: List[MergeRequest]) = Future.sequence(requests.map(acceptMergeRequest(_)))
}



case class Gitlab(baseUrl: String, token: String) extends GitlabAPI
