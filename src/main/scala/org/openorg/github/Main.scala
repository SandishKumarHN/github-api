package org.openorg.github

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import org.openorg.github.app.GitHubApiCacheService

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

object Main extends App {
  val system: ActorSystem = ActorSystem("Github-API-Cache-Service")

  implicit val ec: ExecutionContext = system.dispatcher

  val config: Config = ConfigFactory.load()

  val launchApiFuture: Future[Unit] = Future {
    val gitHubApiCacheService = new GitHubApiCacheService(config)
    gitHubApiCacheService.launchAllApis()
  }

  val result = for {
    _ <- launchApiFuture
  } yield ()

  Await.result(result, Duration.Inf)

  system.terminate()
}
