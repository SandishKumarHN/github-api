package org.openorg.github.utils

import com.typesafe.config.Config
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}
import org.apache.http.impl.client.HttpClients

class GithubApiUtils(config: Config) {
  def requestDetails(): RequestDetails = {
    RequestDetails(
      token = config.getString("github-api-token"),
      org = config.getString("github-api-organization"),
      apiUrl = s"https://api.github.com/",
      headerAccept = "application/vnd.github+json",
      headerApiVersion = config.getString("github-api-header-version"),
      authorizationHeader = s"Bearer ${config.getString("github-api-token")}"
    )
  }

  // Function to send a request to GitHub API
  def sendRequest(url: String): CloseableHttpResponse = {
    val request = new HttpGet(url)
    request.addHeader("Accept", "application/json")
    request.addHeader("Authorization", s"Bearer ${config.getString("github-api-token")}")
    HttpClients.createDefault().execute(request)
  }

  case class RequestDetails(token: String, org: String, apiUrl: String, headerAccept: String, headerApiVersion: String,
                            authorizationHeader: String)

}
