package kindSir.gitlab

import dispatch.Defaults._
import dispatch._
import kindSir.models._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.util.{Success, Try}
import java.net.URLEncoder

trait GitlabAPI {

  val baseUrl: String
  val token: String
  val apiVersion: Int

  def groups(): Future[List[Group]]
  def group(groupName: String): Future[Group]
  def projectConfig(project: Project): Future[ProjectConf]
  def fetchMergeRequests(project: Project, page: Int = 1): Future[List[MergeRequest]]
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
    val all = api(s"/api/v$apiVersion/groups/?per_page=100")
    Http.default(all > as.String) map { str =>
      parse(str) match {
        case list@JArray(_) => Group.parseList(list).get
        case _ => throw new RuntimeException("No groups found")
      }
    }
  }

  def group(groupName: String): Future[Group] = {
    val groupNameEncoded = URLEncoder.encode(groupName, "UTF-8")
    val groupsUrl = api(s"/api/v$apiVersion/groups/$groupNameEncoded")
    Http.default(groupsUrl > as.String) map { string => Group.parse(parse(string)).get }
  }

  def projectConfig(project: Project): Future[ProjectConf] = {
    val treeUrl = api(s"/api/v$apiVersion/projects/${project.id}/repository/tree?per_page=1000")
    Http.default(treeUrl > as.String) map { string =>
      parse(string) match {
        case list@JArray(_) => File.parseList(list).get
        case _ => throw new RuntimeException("No projects found")
      }
    } map { files =>
      files.filter(_.name equalsIgnoreCase ".kind_sir.conf").head
    } flatMap { file =>
      val confUrl = apiVersion match {
        case 1 | 2 | 3 => api(s"/api/v$apiVersion/projects/${project.id}/repository/raw_blobs/${file.id}")
        case _ => api(s"/api/v$apiVersion/projects/${project.id}/repository/blobs/${file.id}/raw")
      }
      Http.default(confUrl > as.String) map { str => ProjectConf.parse(parse(str)).get }
    }
  }

  def fetchMergeRequests(project: Project, page: Int = 1): Future[List[MergeRequest]] = {
    val requestsUrl = api(s"/api/v$apiVersion/projects/${project.id}/merge_requests?page=$page&state=opened&per_page=100&wip=no")
    Http.default(requestsUrl).flatMap { res =>
      (parse(res.getResponseBody), Try(res.getHeader("X-Next-Page").toInt)) match {
        case (list@JArray(_), Success(nextPage)) =>
          fetchMergeRequests(project, nextPage).map { nextPageRequests =>
            MergeRequest.parseList(list).get ++ nextPageRequests
          }
        case (list@JArray(_), _) => Future.successful(MergeRequest.parseList(list).get)
        case _ => throw new RuntimeException(s"No merge requests for project ${project.name} found")
      }
    }
  }

  def acceptMergeRequest(request: MergeRequest): Future[String] = {
    val acceptUrl = apiVersion match {
      case 1 | 2 | 3 => api(s"/api/v$apiVersion/projects/${request.projectId}/merge_requests/${request.id}/merge?should_remove_source_branch=true").PUT
      case _ => api(s"/api/v$apiVersion/projects/${request.projectId}/merge_requests/${request.iid}/merge?should_remove_source_branch=true").PUT
    }
    Http.default(acceptUrl > as.String)
  }


  def fetchLatestBuilds(projectId: Integer): Future[List[Build]] = {
    val commitsUrl = apiVersion match {
      case 1 | 2 | 3 => api(s"/api/v$apiVersion/projects/$projectId/builds?scope=success")
      case _ => api(s"/api/v$apiVersion/projects/$projectId/jobs?scope=success")
    }
    Http.default(commitsUrl > as.String) map { str =>
      parse(str) match {
        case list@JArray(_) => Build.parseList(list).get
        case _ => throw new RuntimeException(s"No build found for project $projectId")
      }
    }
  }

  def fetchCommitsFor(request: MergeRequest): Future[List[Commit]] = {
    val commitsUrl = apiVersion match {
      case 1 | 2 | 3 => api(s"/api/v$apiVersion/projects/${request.projectId}/merge_requests/${request.id}/commits")
      case _ => api(s"/api/v$apiVersion/projects/${request.projectId}/merge_requests/${request.iid}/commits")
    }
    Http.default(commitsUrl > as.String) map { str =>
      parse(str) match {
        case list@JArray(_) => Commit.parseList(list).get
        case _ => throw new RuntimeException(s"No commits found for merge request ${request.id}")
      }
    }
  }
}
