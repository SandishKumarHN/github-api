package org.openorg.github.service

import com.typesafe.scalalogging.LazyLogging
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.util.EntityUtils
import org.openorg.github.utils.GithubApiUtils

import scala.collection.concurrent.TrieMap

class GithubApiDownloader(requestHandler: GithubApiUtils) extends LazyLogging {
  var pageResponseCache: TrieMap[String, Map[String, String]] = TrieMap.empty

  def cacheAndFlatten(apiUrl: String): Unit = {
    val initialUrl = apiUrl
    val initialResponse = requestHandler.sendRequest(initialUrl)
    fetchAllPages(apiUrl, initialUrl, initialResponse)
  }

  def fetchAllPages(url: String, pageUrl: String, response: CloseableHttpResponse): Unit = {
    response.getStatusLine.getStatusCode match {
      case 200 =>
        val responseBody = EntityUtils.toString(response.getEntity)
        addToCache(url, pageUrl, responseBody)
        logger.info(s"Crawling pages, url: ${pageUrl}, cached endpoints : ${pageResponseCache.size}")
        val nextPageUrl = getNextPageUrl(response)
        nextPageUrl match {
          case Some(nextUrl) if !pageResponseCache.contains(nextUrl) =>
            fetchAllPages(url, nextUrl, requestHandler.sendRequest(nextUrl))
          case _ =>
            logger.info(s"Crawling pages, No more pages. total endpoints: ${pageResponseCache.size}")
        }

      case _ =>
        logger.info(s"Failed to fetch data from $pageUrl. HTTP status code: ${response.getStatusLine.getStatusCode}")
    }
  }

  def getNextPageUrl(response: CloseableHttpResponse): Option[String] = {
    Option(response.getFirstHeader("Link"))
      .map(_.getValue)
      .flatMap(linkHeaderValue => {
        val nextPattern = """<([^>]+)>;\s*rel="next"""".r
        nextPattern.findFirstMatchIn(linkHeaderValue).map(_.group(1))
      })
  }

  def addToCache(key: String, valueKey: String, value: String): Unit = {
    pageResponseCache.get(key) match {
      case Some(valueMap) =>
        val updatedValueMap = valueMap + (valueKey -> value)
        pageResponseCache.update(key, updatedValueMap)

      case None =>
        val newValueMap = Map(valueKey -> value)
        pageResponseCache += (key -> newValueMap)
    }
  }
}
