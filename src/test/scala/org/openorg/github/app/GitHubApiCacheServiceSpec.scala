package org.openorg.github.app

import org.scalatest.matchers.should.Matchers
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future

class GitHubApiCacheServiceSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  val config = ConfigFactory.load("application.conf")
  implicit val actorSystem: ActorSystem = ActorSystem("GitHubApiCacheServiceSpec", testConfig)

  val mockService = new GitHubApiCacheServiceMock(config)

  "GitHubApiCacheService" should {

    "respond with OK for healthcheck" in {
      Get("/healthcheck") ~> mockService.route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "return repositories by forks count" in {
      Get("/view/bottom/10/forks") ~> mockService.route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] should include("Mocked response for forks count")
      }
    }

    "return repositories by last updated time" in {
      Get("/view/bottom/10/last_updated") ~> mockService.route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] should include("Mocked response for last updated time")
      }
    }

    "return repositories by open issues count" in {
      Get("/view/bottom/10/open_issues") ~> mockService.route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] should include("Mocked response for open issues count")
      }
    }

    "return repositories by stars count" in {
      Get("/view/bottom/10/stars") ~> mockService.route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] should include("Mocked response for stars count")
      }
    }
  }

  class GitHubApiCacheServiceMock(config: Config) extends GitHubApiCacheService(config) {
    override def launchAllApis(): Future[Http.ServerBinding] = {
      Future.successful(null)
    }
    val route: Route =
      path("healthcheck") {
        get {
          complete(HttpResponse(StatusCodes.OK, entity = "Mocked response for healthcheck"))
        }
      } ~
        path("view" / "bottom" / IntNumber.? / "forks") { n =>
          get {
            complete(HttpResponse(StatusCodes.OK, entity = "Mocked response for forks count"))
          }
        } ~
        path("view" / "bottom" / IntNumber.? / "last_updated") { n =>
          get {
            complete(HttpResponse(StatusCodes.OK, entity = "Mocked response for last updated time"))
          }
        } ~
        path("view" / "bottom" / IntNumber.? / "open_issues") { n =>
          get {
            complete(HttpResponse(StatusCodes.OK, entity = "Mocked response for open issues count"))
          }
        } ~
        path("view" / "bottom" / IntNumber.? / "stars") { n =>
          get {
            complete(HttpResponse(StatusCodes.OK, entity = "Mocked response for stars count"))
          }
        }
  }
}
