package org.openorg.github.service

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.WordSpec
import org.scalatestplus.mockito.MockitoSugar

class CustomRepositoryViewServicesSpec extends WordSpec with Matchers with MockitoSugar {

  "CustomRepositoryViewServices" should {
    val service = new CustomRepositoryViewServices

    "return bottom N repositories by stars_count" in {
      val jsonString = "[{\"id\":1,\"name\":\"repo1\",\"full_name\":\"owner/repo1\",\"open_issues\":1,\"forks_count\":2,\"stargazers_count\":3,\"updated_at\":\"2023-11-15T12:00:00Z\",\"created_at\":\"2023-11-15T11:00:00Z\"}]"

      val result = service.bottomNReposByStarsCount(1, jsonString)
      result should contain theSameElementsAs List(("owner/repo1", 3))
    }

    "return bottom N repositories by open_issues_count" in {
      val jsonString = "[{\"id\":1,\"name\":\"repo1\",\"full_name\":\"owner/repo1\",\"open_issues\":1,\"forks_count\":2,\"stargazers_count\":3,\"updated_at\":\"2023-11-15T12:00:00Z\",\"created_at\":\"2023-11-15T11:00:00Z\"}]"

      val result = service.bottomNReposByOpenIssuesCount(1, jsonString)
      result should contain theSameElementsAs List(("owner/repo1", 1))
    }

    "return bottom N repositories by forks_count" in {
      val jsonString = "[{\"id\":1,\"name\":\"repo1\",\"full_name\":\"owner/repo1\",\"open_issues\":1,\"forks_count\":2,\"stargazers_count\":3,\"updated_at\":\"2023-11-15T12:00:00Z\",\"created_at\":\"2023-11-15T11:00:00Z\"}]"

      val result = service.bottomNReposByForksCount(1, jsonString)
      result should contain theSameElementsAs List(("owner/repo1", 2))
    }

    "return bottom N repositories by last updated_at" in {
      val jsonString = "[{\"id\":1,\"name\":\"repo1\",\"full_name\":\"owner/repo1\",\"open_issues\":1,\"forks_count\":2,\"stargazers_count\":3,\"updated_at\":\"2023-11-15T12:00:00Z\",\"created_at\":\"2023-11-15T11:00:00Z\"}]"

      val result = service.bottomNReposByLastUpdateAt(1, jsonString)
      result should contain theSameElementsAs List(("owner/repo1", "2023-11-15T12:00:00Z"))
    }
  }
}
