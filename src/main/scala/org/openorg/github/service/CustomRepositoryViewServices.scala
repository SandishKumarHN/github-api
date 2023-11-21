package org.openorg.github.service

import com.typesafe.scalalogging._
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.decode

import java.text.SimpleDateFormat
import java.util.{Date, Locale}

case class RepositoryDetails(id: BigInt, name: String, full_name: String, open_issues: BigInt, forks_count: BigInt,
                             stargazers_count: BigInt, updated_at: String, created_at: String) {
}

class CustomRepositoryViewServices extends LazyLogging {

  implicit val decoder: Decoder[RepositoryDetails] = deriveDecoder

  def bottomNReposByStarsCount(topN: Int, jsonString: String): List[(String, BigInt)] = {
    val repositoryDetailsList = decodeJsonString(jsonString)
    logger.info(s"bottom ${topN} repositories by stars_count, from cached repositories of size ${repositoryDetailsList.size}")
    val bottomNRepos = repositoryDetailsList
      .sortBy(r => (r.stargazers_count, r.full_name)) // Sort by stargazers count
      .take(topN) // Take top N repositories

    bottomNRepos.map { repositoryDetails =>
      (repositoryDetails.full_name, repositoryDetails.stargazers_count)
    }
  }

  def bottomNReposByOpenIssuesCount(topN: Int, jsonString: String): List[(String, BigInt)] = {
    val repositoryDetailsList = decodeJsonString(jsonString)
    logger.info(s"bottom ${topN} repositories by open_issues_count, from cached repos of size ${repositoryDetailsList.size}")
    val bottomNRepos = repositoryDetailsList
      .sortBy(r => (r.open_issues, r.full_name)) // Sort by open issues count
      .take(topN) // Take top N repositories

    bottomNRepos.map { repositoryDetails =>
      (repositoryDetails.full_name, repositoryDetails.open_issues)
    }
  }

  def bottomNReposByForksCount(topN: Int, jsonString: String): List[(String, BigInt)] = {
    val repositoryDetailsList = decodeJsonString(jsonString)
    logger.info(s"bottom ${topN} repositories by forks_count, from cached repositories of size ${repositoryDetailsList.size}")
    val bottomNRepos = repositoryDetailsList
      .sortBy(r => (r.forks_count, r.full_name)) // Sort by forks count
      .take(topN) // Take top N repositories

    bottomNRepos.map { repositoryDetails =>
      (repositoryDetails.full_name, repositoryDetails.forks_count)
    }
  }

  def decodeJsonString[T: Decoder](jsonString: String): List[T] = {
    decode[List[T]](jsonString) match {
      case Right(details) => details
      case Left(error) =>
        logger.error(s"Failed to parse JSON: $error")
        List.empty
    }
  }

  def bottomNReposByLastUpdateAt(topN: Int, jsonString: String): List[(String, String)] = {
    val repositoryDetailsList = decodeJsonString(jsonString)
    logger.info(s"bottom ${topN} repositories by last updated_at, from cached repositories of size ${repositoryDetailsList.size}")
    val bottomNRepos = repositoryDetailsList.sortWith {
      case (r1, r2) =>
        (parseDate(r1.updated_at), parseDate(r2.updated_at)) match {
          case (Some(a), Some(b)) =>
            if (a.equals(b)) {
              r1.full_name < r2.full_name
            } else {
              a.before(b)
            }
          case _ => false
        }
    }.take(topN) // Take top N repositories

    bottomNRepos.map { repositoryDetails =>
      (repositoryDetails.full_name, repositoryDetails.updated_at)
    }
  }

  def parseDate(dateString: String): Option[Date] = {
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    try {
      val parsedDate = dateFormat.parse(dateString)
      Some(parsedDate)
    } catch {
      case _: Throwable => None
    }
  }
}

