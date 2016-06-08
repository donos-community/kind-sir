package kindSir.gitlab

import dispatch.Defaults._
import dispatch._
import kindSir.models._
import org.json4s._
import org.json4s.jackson.JsonMethods._

trait GitlabAPI {

  val baseUrl: String
  val token: String

  def groups(): Future[List[Group]]
  def group(groupName: String): Future[Group]
  def projectConfig(project: Project): Future[ProjectConf]
  def fetchMergeRequests(project: Project): Future[List[MergeRequest]]
  def acceptMergeRequest(request: MergeRequest): Future[String]

  def acceptMergeRequests(requests: List[MergeRequest]) = Future.sequence(requests.map(acceptMergeRequest(_)))
}


case class Gitlab(baseUrl: String, token: String) extends GitlabAPI {

  private def api(apiHandle: String): Req = {
    url(s"$baseUrl$apiHandle").addHeader("PRIVATE-TOKEN", token)
  }

  def groups(): Future[List[Group]] = {
    val all = api("/api/v3/groups/")
    Http(all OK as.String) map { str =>
      parse(str) match {
        case list@JArray(_) => Group.parseList(list).get
        case _ => throw new RuntimeException("No groups found")
      }
    }
  }

  def group(groupName: String): Future[Group] = {
    val groupsUrl = api(s"/api/v3/groups/$groupName")
    Http(groupsUrl OK as.String) map { string => Group.parse(parse(string)).get }
  }

  def projectConfig(project: Project): Future[ProjectConf] = {
    val treeUrl = api(s"/api/v3/projects/${project.id}/repository/tree")
    Http(treeUrl OK as.String) map { string =>
      parse(string) match {
        case list@JArray(_) => File.parseList(list).get
        case _ => throw new RuntimeException("No projects found")
      }
    } map { files =>
      files.filter(_.name equalsIgnoreCase ".kind_sir.conf").head
    } flatMap { file =>
      val confUrl = api(s"/api/v3/projects/${project.id}/repository/raw_blobs/${file.id}")
      Http(confUrl OK as.String) map { str => ProjectConf.parse(parse(str)).get }
    }
  }

  def fetchMergeRequests(project: Project): Future[List[MergeRequest]] = {
    val requestsUrl = api(s"/api/v3/projects/${project.id}/merge_requests?state=opened")
    Http(requestsUrl OK as.String) map { str =>
      parse(str) match {
        case list@JArray(_) => MergeRequest.parseList(list).get
        case _ => throw new RuntimeException(s"No merge requests for project ${project.name} found")
      }
    }
  }

  def acceptMergeRequest(request: MergeRequest): Future[String] = {
    val acceptUrl = api(
      s"/api/v3/projects/${request.projectId}/merge_request/${request.id}/merge?should_remove_source_branch=true").PUT
    Http(acceptUrl OK as.String)
  }
}
