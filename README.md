# GitHub API Cache Service

## Overview

The GitHub API Cache Service is implemented in Scala using Akka HTTP. It serves as a cache for GitHub API responses and provides cached data through a set of RESTful APIs. The service periodically(1 hour) refreshes the cache for specified GitHub API endpoints.

## Features

- **RESTful APIs:** Provides several RESTful APIs to retrieve cached GitHub data, including repositories, members, and organization details.
- **Caching:** The service periodically(1 hour) refreshes the cache for specified GitHub API endpoints.

## Usage

### Installation

To use the GitHub API Cache Service, follow these steps:

1. Ensure you have Scala(2.13.12) and SBT installed(1.9.7).
2. export GITHUB_API_TOKEN to your [Github API token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#creating-a-fine-grained-personal-access-token)
3. Clone the repository: `git clone https://github.com/SandishKumarHN/github-api.git`
4. Navigate to the project directory: `cd github-api`
5. Run the service: `sh scripts/run.sh`

### API Endpoints

#### Health Check

- **Endpoint:** `/healthcheck`
- **Method:** `GET`
- **Description:** Performs a health check and returns a 200 OK status if the service is running.
- **Example:** ```curl -X GET http://localhost:8090/healthcheck```
- 
#### Github Organization APIs - Cached

The following APIs provide information about GitHub organizations:

- **Organization Details:**
  - **Endpoint:** `/orgs/<organization-name>`
  - **Method:** `GET`
  - **Description:** Retrieves details about the specified GitHub organization.
  - **Example:** ```curl -X GET http://localhost:8090/orgs/Netflix```

- **Organization Members:**
  - **Endpoint:** `/orgs/<organization-name>/members`
  - **Method:** `GET`
  - **Description:** Retrieves the members of the specified GitHub organization.
  - **Example:** ```curl -X GET http://localhost:8090/orgs/Netflix/members```

- **Organization Repositories:**
  - **Endpoint:** `/orgs/<organization-name>/repos`
  - **Method:** `GET`
  - **Description:** Retrieves the repositories of the specified GitHub organization.
  - **Example:** ```curl -X GET http://localhost:8090/orgs/Netflix/repos```


#### Organization stats APIs - Cached
The following APIs provide information about repositories:

- **Forks Count:**
    - **Endpoint:** `/view/bottom/<n>/forks`
    - **Method:** `GET`
    - **Description:** Retrieves the bottom `n` repositories by forks count.
    - **Example:** ```curl -X GET http://localhost:8090/view/bottom/10/forks```

- **Last Update:**
    - **Endpoint:** `/view/bottom/<n>/last_updated`
    - **Method:** `GET`
    - **Description:** Retrieves the bottom `n` repositories by last update timestamp.
    - **Example:** ```curl -X GET http://localhost:8090/view/bottom/10/last_updated```

- **Open Issues Count:**
    - **Endpoint:** `/view/bottom/<n>/open_issues`
    - **Method:** `GET`
    - **Description:** Retrieves the bottom `n` repositories by open issues count.
    - **Example:** ```curl -X GET http://localhost:8090/view/bottom/10/open_issues```

- **Stars Count:**
    - **Endpoint:** `/view/bottom/<n>/stars`
    - **Method:** `GET`
    - **Description:** Retrieves the bottom `n` repositories by stars count.
    - **Example:** ```curl -X GET http://localhost:8090/view/bottom/10/stars```

#### Proxy API - Proxy
For any other GitHub API endpoint, you can use the proxy API:

- **Endpoint:** `/<path-segments...>`
- **Method:** `GET`
- **Description:** Proxies the request to the GitHub API and returns the response.
- **Example:** ```curl -X GET http://localhost:8090/user/repos```

### Configuration

The service is configurable through the `application.conf` file. Adjust the configuration parameters, such as the interface and port, to suit your needs.

### Design Decisions

- #### Languages
  - The job description mentioned preference of Scala, I saw this as an opportunity to showcase my proficiency in Scala.
  
- #### API
  - Spring Boot API
    - Well-established framework with a large community. 
    - Rich ecosystem and extensive documentation. 
    - Convention over configuration, making development faster.
  - Akka HTTP
    - Reactive and actor-based model, suitable for concurrent and scalable applications. 
    - Lightweight and efficient, especially for handling a large number of concurrent requests. 
    - Good support for asynchronous programming.
  - Choice
    - Choose Akka HTTP for the reasons like Concurrency and Scalability, Asynchronous Programming.
    - lightweight and reactive nature make it a good fit for our high-concurrency caching service.
    
- #### Data Storage Considerations
  - Redis
    - In-memory data store, providing fast read access. 
    - Suitable for caching, but may require additional configuration for persistence.
  - Embedded Databases (DuckDB, SQLite):
    - Lightweight and can be embedded within the application.
    - Considerable for experimentation but might be suboptimal for frequent upserts and reads.
  - TreeMap 
    - Thread-safe and lightweight implementation.
    - Provides efficient in-memory storage for caching aggregated views.
  - Choice
    - I chose TreeMap for its thread safety, lightweight nature, and efficiency in in-memory storage.
    - It simplifies our implementation, providing a good balance between performance and simplicity.
