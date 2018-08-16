package kindSir.gitlab

import dispatch.Defaults._
import dispatch._
import kindSir.models._
import org.json4s._
import org.json4s.jackson.JsonMethods._

trait GitlabAPI {

  val baseUrl: String
  val token: String
  val apiVersion: Int

  def groups(): Future[List[Group]]
  def group(groupName: String): Future[Group]
  def projectConfig(project: Project): Future[ProjectConf]
  def fetchMergeRequests(project: Project): Future[List[MergeRequest]]
  def acceptMergeRequest(request: MergeRequest): Future[String]
  def fetchCommitsFor(request: MergeRequest): Future[List[Commit]]
  def fetchLatestBuilds(projectId: Integer): Future[List[Build]]

  def acceptMergeRequests(requests: List[MergeRequest]) = Future.sequence(requests.map(acceptMergeRequest(_)))
}


case class Gitlab(baseUrl: String, token: String, apiVersion: Int) extends GitlabAPI {

  private def api(apiHandle: String): Req = {
    url(s"$baseUrl$apiHandle").addHeader("PRIVATE-TOKEN", token)
  }

  def groups(): Future[List[Group]] = {
    val all = api(s"/api/v${apiVersion}/groups/")
    Http(all OK as.String) map { str =>
      parse(str) match {
        case list@JArray(_) => Group.parseList(list).get
        case _ => throw new RuntimeException("No groups found")
      }
    }
  }

  def group(groupName: String): Future[Group] = {
    val groupsUrl = api(s"/api/v${apiVersion}/groups/$groupName")
    Http(groupsUrl OK as.String) map { string => Group.parse(parse(string)).get }
  }

  def projectConfig(project: Project): Future[ProjectConf] = {
    val treeUrl = api(s"/api/v${apiVersion}/projects/${project.id}/repository/tree")
    Http(treeUrl OK as.String) map { string =>
      parse(string) match {
        case list@JArray(_) => File.parseList(list).get
        case _ => throw new RuntimeException("No projects found")
      }
    } map { files =>
      files.filter(_.name equalsIgnoreCase ".kind_sir.conf").head
    } flatMap { file =>
      val confUrl = api(s"/api/v${apiVersion}/projects/${project.id}/repository/raw_blobs/${file.id}")
      Http(confUrl OK as.String) map { str => ProjectConf.parse(parse(str)).get }
    }
  }

  def fetchMergeRequests(project: Project): Future[List[MergeRequest]] = {
    val requestsUrl = api(s"/api/v${apiVersion}/projects/${project.id}/merge_requests?state=opened")
    Http(requestsUrl OK as.String) map { str =>
      parse(str) match {
        case list@JArray(_) => MergeRequest.parseList(list).get
        case _ => throw new RuntimeException(s"No merge requests for project ${project.name} found")
      }
    }
  }

  def acceptMergeRequest(request: MergeRequest): Future[String] = {
    val acceptUrl = api(
      s"/api/v${apiVersion}/projects/${request.projectId}/merge_requests/${request.id}/merge?should_remove_source_branch=true").PUT
    Http(acceptUrl OK as.String)
  }


  def fetchLatestBuilds(projectId: Integer): Future[List[Build]] = {
    val commitsUrl = api(s"/api/v${apiVersion}/projects/$projectId/builds?scope=success")
    Http(commitsUrl OK as.String) map { str =>
      parse(str) match {
        case list@JArray(_) => Build.parseList(list).get
        case _ => throw new RuntimeException(s"No build found for project $projectId")
      }
    }
  }

  def fetchCommitsFor(request: MergeRequest): Future[List[Commit]] = {
    val commitsUrl = api(s"/api/v${apiVersion}/projects/${request.projectId}/merge_requests/${request.id}/commits")
    Http(commitsUrl OK as.String) map { str =>
      parse(str) match {
        case list@JArray(_) => Commit.parseList(list).get
        case _ => throw new RuntimeException(s"No commits found for merge request ${request.id}")
      }
    }
  }
}
