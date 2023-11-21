package org.openorg.github.service

import java.io.ByteArrayInputStream
import org.apache.http.{Header, HttpEntity, StatusLine}
import org.apache.http.client.methods.CloseableHttpResponse
import org.mockito.Mockito.when
import org.openorg.github.utils.GithubApiUtils
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class GithubApiDownloaderSpec extends AnyWordSpec with Matchers with MockitoSugar {

  private def mockEntity(content: String, statusCode: Int = 200, headerValue: String = ""): CloseableHttpResponse = {
    val mockEntity = mock[HttpEntity]
    when(mockEntity.getContent).thenReturn(new ByteArrayInputStream(content.getBytes))

    val mockResponse = mock[StatusLine]
    when(mockResponse.getStatusCode).thenReturn(statusCode)

    val mockHttpResponse = mock[CloseableHttpResponse]
    when(mockHttpResponse.getStatusLine).thenReturn(mockResponse)
    when(mockHttpResponse.getEntity).thenReturn(mockEntity)

    val mockHeader = mock[Header]
    when(mockHeader.getName).thenReturn("Link")
    when(mockHeader.getValue).thenReturn(headerValue)
    when(mockHttpResponse.getFirstHeader("Link")).thenReturn(mockHeader)

    mockHttpResponse
  }

  "GithubApiDownloader" should {
    "addToCache should insert a new key-value pair when the key doesn't exist" in {
      val downloader = new GithubApiDownloader(mock[GithubApiUtils])
      val key = "key"
      val valueKey = "valueKey"
      val value = "value"

      downloader.addToCache(key, valueKey, value)

      downloader.pageResponseCache.get(key) shouldBe Some(Map(valueKey -> value))
    }

    "addToCache should add to the existing value map when the key exists" in {
      val downloader = new GithubApiDownloader(mock[GithubApiUtils])
      val key = "key"
      val valueKey1 = "valueKey1"
      val value1 = "value1"
      val valueKey2 = "valueKey2"
      val value2 = "value2"

      downloader.addToCache(key, valueKey1, value1)
      downloader.addToCache(key, valueKey2, value2)

      downloader.pageResponseCache.get(key) shouldBe Some(Map(valueKey1 -> value1, valueKey2 -> value2))
    }

    "getNextPageUrl should return the next page URL if present in the Link header" in {
      val downloader = new GithubApiDownloader(mock[GithubApiUtils])
      val nextPageUrl = "https://api.github.com/resource?page=2"
      val response = mockEntity(s"""<$nextPageUrl>; rel="next", <https://api.github.com/resource?page=5>; rel="last"""",
        headerValue = s"""<$nextPageUrl>; rel="next", <https://api.github.com/resource?page=5>; rel="last"""")

      downloader.getNextPageUrl(response) shouldBe Some(nextPageUrl)
    }

    "getNextPageUrl should return None if the Link header is missing or malformed" in {
      val downloader = new GithubApiDownloader(mock[GithubApiUtils])
      val response1 = mockEntity("""<https://api.github.com/resource?page=2>; rel="prev"""",
        headerValue = s"""rel="next", <https://api.github.com/resource?page=5>; rel="last"""")
      val response2 = mockEntity("""<https://api.github.com/resource?page=5>""",
        headerValue = s"""rel="next", <https://api.github.com/resource?page=5>; rel="last"""")

      downloader.getNextPageUrl(response1) shouldBe None
      downloader.getNextPageUrl(response2) shouldBe None
    }

    "fetchAllPages should fetch all pages and update cache" in {
      val requestHandler = mock[GithubApiUtils]
      val downloader = new GithubApiDownloader(requestHandler)
      val nextPageUrl = "https://api.github.com/resource?page=2"

      val apiUrl = "https://api.github.com/resource"
      val initialResponse = mockEntity( """["item1", "item2"]""", 200,
        headerValue = s"""<$nextPageUrl>; rel="next", <https://api.github.com/resource?page=5>; rel="last"""")

      val nextPageResponse = mockEntity("""["item3", "item4"]""")

      when(requestHandler.sendRequest(apiUrl)).thenReturn(initialResponse)
      when(requestHandler.sendRequest(nextPageUrl)).thenReturn(nextPageResponse)

      downloader.fetchAllPages("resource", apiUrl, initialResponse)

      downloader.pageResponseCache.get("resource") shouldBe Some(Map(("https://api.github.com/resource" -> """["item1", "item2"]"""), ("https://api.github.com/resource?page=2" -> """["item3", "item4"]""")))
    }

    "cacheAndFlatten should cache API response and return flattened response" in {
      val requestHandler = mock[GithubApiUtils]
      val downloader = new GithubApiDownloader(requestHandler)

      val apiUrl = "https://api.github.com/resource"
      val response = mockEntity("""["item1", "item2"]""")

      when(requestHandler.sendRequest(apiUrl)).thenReturn(response)

      downloader.cacheAndFlatten(apiUrl)

      downloader.pageResponseCache.get(apiUrl) shouldBe Some(Map("https://api.github.com/resource" -> """["item1", "item2"]"""))
    }
  }
}
