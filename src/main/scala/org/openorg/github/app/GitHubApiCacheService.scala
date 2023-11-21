package org.openorg.github.app

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.util.EntityUtils
import org.openorg.github.service.{CustomRepositoryViewServices, GithubApiDownloader}
import org.openorg.github.utils.GithubApiUtils
import io.circe.Json
import io.circe.parser._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class GitHubApiCacheService(config: Config) extends LazyLogging {

  implicit val system: ActorSystem = ActorSystem("GitHubApiCacheService")

  val requestHandler = new GithubApiUtils(config)
  val requestDetails = requestHandler.requestDetails()
  val apiUrl = requestDetails.apiUrl

  // Cache service to store the responses
  val cacheService = new GithubApiDownloader(requestHandler)

  val customRepositoryViewServices: CustomRepositoryViewServices = new CustomRepositoryViewServices()

  val forksApi: PathMatcher1[Option[Int]] = "view" / "bottom" / IntNumber.? / "forks"
  val lastUpdateApi: PathMatcher1[Option[Int]] = "view" / "bottom" / IntNumber.? / "last_updated"
  val openIssuesApi: PathMatcher1[Option[Int]] = "view" / "bottom" / IntNumber.? / "open_issues"
  val starsApi: PathMatcher1[Option[Int]] = "view" / "bottom" / IntNumber.? / "stars"

  val cachedApis: Set[String] = config.getStringList("cached-apis")
    .toArray(Array.empty[String]).map(endpoint => s"${apiUrl}${endpoint}").toSet

  // Schedule to refresh cache periodically.
  system.scheduler.schedule(0.seconds, 1.hour) {
    cacheService.pageResponseCache.clear()
    cachedApis.map{
      endpoint =>
        logger.info(s"Refreshing cache for endpoint, ${endpoint}")
        retrieveOrCacheEndpoint(endpoint)
    }
  }

  def launchAllApis(): Future[Http.ServerBinding] = {
    logger.info("Launching Cached Github API service......")

    val route = pathPrefix("healthcheck") {
      pathEndOrSingleSlash {
        get {
          complete(OK)
        }
      }
    } ~ pathPrefix(forksApi) { n =>
      pathEndOrSingleSlash {
        get {
          val jsonString = retrieveOrCacheAndProcessJson(s"${apiUrl}orgs/Netflix/repos")
          val repoDetailsList = customRepositoryViewServices.bottomNReposByForksCount(n.getOrElse(10), jsonString)
          complete {
            HttpResponse(status = StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, formatData(repoDetailsList)))
          }
        }
      }
    } ~ pathPrefix(lastUpdateApi) { n =>
      pathEndOrSingleSlash {
        get {
          val jsonString = retrieveOrCacheAndProcessJson(s"${apiUrl}orgs/Netflix/repos")
          val repoDetailsList = customRepositoryViewServices.bottomNReposByLastUpdateAt(n.getOrElse(10), jsonString)
          complete {
            HttpResponse(status = StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, formatData(repoDetailsList)))
          }
        }
      }
    } ~ pathPrefix(openIssuesApi) { n =>
      pathEndOrSingleSlash {
        get {
          val jsonString = retrieveOrCacheAndProcessJson(s"${apiUrl}orgs/Netflix/repos")
          val repoDetailsList = customRepositoryViewServices.bottomNReposByOpenIssuesCount(n.getOrElse(10), jsonString)
          complete {
            HttpResponse(status = StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, formatData(repoDetailsList)))
          }
        }
      }
    } ~ pathPrefix(starsApi) { n =>
      get {
        pathEndOrSingleSlash {
          val jsonString = retrieveOrCacheAndProcessJson(s"${apiUrl}orgs/Netflix/repos")
          val repoDetailsList = customRepositoryViewServices.bottomNReposByStarsCount(n.getOrElse(10), jsonString)
          complete {
            HttpResponse(status = StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, formatData(repoDetailsList)))
          }
        }
      }
    } ~
      pathPrefix(Segments) { segments =>
        get {
          val endpoint = segments.mkString("/")
          val fullUrl = s"$apiUrl$endpoint"
          logger.info(s"Retrieve cached response or send Github API request, ${fullUrl}")
          complete(retrieveOrCacheEndpoint(fullUrl))
        }
    }

    val bindingFuture = Http().bindAndHandle(route, config.getString("interface"), config.getInt("port"))
    logger.info(s"Server online at http://${config.getString("interface")}:${config.getInt("port")}")

    bindingFuture.foreach { serverBinding =>
      logger.info(s"Address bound to ${serverBinding.localAddress}")

      sys.addShutdownHook {
        logger.info("Shutting down server...")
        serverBinding.unbind()
        system.terminate()
      }
    }
    bindingFuture
  }

  // Function to cache and flatten the GitHub API response

  def cacheIfNeeded(endpoint: String): Unit = {
    if (!cacheService.pageResponseCache.contains(endpoint) && cachedApis.contains(endpoint)) {
      cacheService.cacheAndFlatten(endpoint)
    }
  }

  def retrieveOrCacheEndpoint(endpoint: String): Future[HttpResponse] = Future {
    try {
      getFromCacheOrGithubApi(endpoint)
    } catch {
      case ex: Exception =>
        logger.error(s"${ex.getMessage}, stack trace: ${ex.printStackTrace()}")
        HttpResponse(
          status = StatusCodes.InternalServerError,
          entity = HttpEntity(ContentTypes.`application/json`, "Internal server error")
        )
    }
  }

  def retrieveOrCacheAndProcessJson(endpoint: String): String = {
    cacheIfNeeded(endpoint)
    cacheService.pageResponseCache.get(endpoint) match {
      case Some(endpointMap) =>
        val jsonArray: Seq[Json] = endpointMap.values.flatMap { jsonString =>
          parse(jsonString).toOption
        }.toList
        val jsonList = jsonArray.flatMap(_.asArray.getOrElse(List.empty))
        if(jsonList.isEmpty) {
          jsonArray.mkString("")
        } else  {
          Json.fromValues(jsonList).spaces2
        }
      case None => null
    }
  }

  def formatData(data: List[(String, Any)]): String = {
    val formattedData = data.map {
      case (repoFullName, count: String) => s"""       ["$repoFullName", "$count"]"""
      case (repoFullName, count) => s"""       ["$repoFullName", $count]"""
    }.mkString(",\n")
    s"[\n$formattedData\n]"
  }

  def getFromCacheOrGithubApi(endpoint: String): HttpResponse = {
    val jsonString = retrieveOrCacheAndProcessJson(endpoint)
    if (jsonString != null) {
      HttpResponse(
        status = StatusCodes.OK,
        entity = HttpEntity(ContentTypes.`application/json`, jsonString)
      )
    } else {
      logger.info(s"Direct request to Github API server, ${endpoint}")
      val response: CloseableHttpResponse = requestHandler.sendRequest(endpoint)
      val statusCode = response.getStatusLine.getStatusCode
      val entity = EntityUtils.toString(response.getEntity)
      HttpResponse(
        status = StatusCodes.getForKey(statusCode).getOrElse(StatusCodes.InternalServerError),
        entity = HttpEntity(ContentTypes.`application/json`, entity)
      )
    }
  }
}
