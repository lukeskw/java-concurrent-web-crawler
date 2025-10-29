# Java Concurrent Web Crawler

## Overview

The **Java Concurrent Web Crawler** is a Spring Boot-based application that provides a concurrent web crawling service with user authentication and authorization. The application allows users to submit keyword-based crawl requests, processes them concurrently, and caches the results for efficient retrieval.

## Features

- **JWT-based Authentication**: Secure user authentication using JSON Web Tokens
- **Role-based Authorization**: Support for different user roles (e.g., ADMIN, USER)
- **Concurrent Web Crawling**: Parallel processing of web crawling tasks
- **Redis Caching**: Efficient caching of crawl states and results
- **PostgreSQL Database**: Persistent storage of users and crawl requests
- **Token Blacklisting**: Secure logout functionality with Redis-based token blacklisting
- **RESTful API**: Clean REST endpoints for crawling and authentication

## Technology Stack

- **Java 25**
- **Spring Boot 3.x**
- **Spring Data JPA**
- **Spring Security**
- **Spring MVC**
- **Lombok**
- **JWT (jjwt)**
- **PostgreSQL 17**
- **Redis 7**
- **Liquibase** (Database migrations)
- **Docker & Docker Compose**

## Project Structure
```

java-concurrent-web-crawler/ 
├── src/ 
│ ├── main/ 
│ │ ├── java/ 
│ │ │ └── com.concurrent_web_crawler/ 
│ │ │ ├── auth/ # Authentication module 
│ │ │ │ ├── dto/ # Data Transfer Objects 
│ │ │ │ ├── model/ # Domain models (UserAccount, Role) 
│ │ │ │ ├── port.out/ # Output ports 
│ │ │ │ ├── repository/ # JPA repositories 
│ │ │ │ ├── security/ # Security configuration 
│ │ │ │ ├── service/ # Business logic 
│ │ │ │ └── web/ # REST controllers 
│ │ │ ├── crawler/ # Web crawler module 
│ │ │ │ ├── dto/ # DTOs for crawling 
│ │ │ │ ├── enumerator/ # Enums (CrawlStatus) 
│ │ │ │ ├── infra.executor/ # Crawl job execution 
│ │ │ │ ├── model/ # Domain models 
│ │ │ │ ├── port.out/ # Output ports 
│ │ │ │ ├── repository/ # JPA repositories 
│ │ │ │ ├── service/ # Crawling business logic 
│ │ │ │ ├── util/ # Utility classes 
│ │ │ │ └── web/ # REST controllers 
│ │ │ ├── shared/ # Shared components 
│ │ │ │ ├── config/ # Application configuration 
│ │ │ │ └── exception/ # Exception handling 
│ │ │ └── CrawlerApplication.java # Application entry point 
│ │ └── resources/ 
│ │ ├── db.changelog/ # Liquibase migrations 
│ │ └── application.properties # Application config 
│ └── test/ 
├── docker-compose.yml 
├── pom.xml 
└── .env.example```
```

## Architecture

The application follows a **Hexagonal Architecture** (Ports and Adapters) pattern:

- **Domain Layer**: Core business logic and models
- **Port Layer**: Interfaces defining boundaries
- **Adapter Layer**: Implementations (Web controllers, Repositories, External services)
- **Shared Layer**: Cross-cutting concerns (Configuration, Exception handling)

## API Endpoints

### Authentication Endpoints

#### POST `/api/auth/login`
Login with username and password.

**Request Body:**

```
json { "username": "string", "password": "string" }
```

**Response (Success):**
```
json { "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", "expiresIn": 3600 }
```

#### POST `/api/auth/logout`
Logout and blacklist the current token.

**Headers:** `Authorization: Bearer <token>`

**Response:**

```
json { "message": "Logged out successfully" }
```

#### GET `/api/auth/me`
Get current authenticated user information.

**Headers:** `Authorization: Bearer <token>`

**Response:**

```
json { "id": 1, "username": "john_doe", "roles": ["ROLE_USER"] }
```

### Crawler Endpoints

#### POST `/api/crawl`
Start a new web crawl for a given keyword.

**Headers:** `Authorization: Bearer <token>`

**Request Body:**

```
json { "keyword": "spring boot tutorial" } 
```

**Response:**

```
json { "crawlId": "abc123xyz" }
```
#### GET `/api/crawl/state/{crawlId}`
Get the current state of a crawl request.

**Headers:** `Authorization: Bearer <token>`

**Response:**

```
json { "id": "abc123xyz", "status": "ACTIVE", "urls": string[] }
``` 

## Database Schema

### UserAccount Table
- `id` (BIGINT, PK)
- `username` (VARCHAR, UNIQUE)
- `email` (VARCHAR, UNIQUE)
- `password` (VARCHAR, hashed)
- `role` (VARCHAR)
- `created_at` (TIMESTAMP)

### CrawlRequest Table
- `id` (BIGINT, PK)
- `keyword_normalized` (VARCHAR)
- `status` (VARCHAR)
- `result_json` (JSONB)
- `created_by` (BIGINT, FK → UserAccount)
- `created_at` (TIMESTAMP)
- `completed_at` (TIMESTAMP)

## Setup and Installation

### Prerequisites

- **Java 25** (or compatible JDK)
- **Maven 3.8+**
- **Docker** and **Docker Compose**

### 1. Clone the Repository

```
bash git clone <repository-url> cd java-concurrent-web-crawler
```

### 2. Configure Environment Variables

Copy the example environment file and update the values:

```
bash cp .env.example .env
``` 

Edit `.env` with your configuration:

```
properties
App DB
APP_DB_HOST=localhost 
APP_DB_PORT=5432 
APP_DB_NAME=app_db 
APP_DB_USER=app_user 
APP_DB_PASSWORD=app_password

Common
DB_DRIVER=org.postgresql.Driver 
LIQUIBASE_CHANGELOG=classpath:db/changelog/db.changelog-master.yaml 
LIQUIBASE_DEFAULT_SCHEMA=public 
SPRING_APP_NAME=crawler
JWT Configuration (IMPORTANT: Use a strong secret with at least 32 characters)
JWT_SECRET=your-secret-key-at-least-32-characters-long 
JWT_ACCESS_TTL_SECONDS=3600 
JWT_REFRESH_TTL_SECONDS=1209600
Redis
REDIS_HOST=localhost REDIS_PORT=6379
Base URL (optional)
BASE_URL=http://localhost:8080
```

### 3. Start Infrastructure with Docker Compose

```
bash docker-compose up -d
``` 

This will start:
- **PostgreSQL** on port `5432`
- **Redis** on port `6380`

### 4. Build the Application

```
bash ./mvnw clean install
```

### 5. Run the Application

```
bash ./mvnw spring-boot:run
``` 

The application will start on `http://localhost:8080`.

## Configuration

### application.properties

The main configuration file is located at `src/main/resources/application.properties`.

Key configurations:

```
properties
Server
server.port=8080
Database
spring.datasource.url=jdbc:postgresql://{APP_DB_HOST:localhost}:{APP_DB_PORT:5432}/{APP_DB_NAME:app_db} 
spring.datasource.username={APP_DB_USER:app_user} 
spring.datasource.password={APP_DB_PASSWORD:app_password} 
spring.datasource.driver-class-name={DB_DRIVER:org.postgresql.Driver}
JPA/Hibernate
spring.jpa.hibernate.ddl-auto=none 
spring.jpa.show-sql=true
Liquibase
spring.liquibase.enabled=true 
spring.liquibase.change-log={LIQUIBASE_CHANGELOG:classpath:db/changelog/db.changelog-master.yaml} 
spring.liquibase.default-schema={LIQUIBASE_DEFAULT_SCHEMA:public}
Redis
spring.data.redis.host={REDIS_HOST:localhost} 
spring.data.redis.port={REDIS_PORT:6379}
JWT
security.jwt.secret={JWT_SECRET} 
security.jwt.access-ttl-seconds={JWT_ACCESS_TTL_SECONDS:3600} 
security.jwt.refresh-ttl-seconds=${JWT_REFRESH_TTL_SECONDS:1209600}
Cache
spring.cache.type=redis
Application
spring.application.name=${SPRING_APP_NAME:crawler}
```

## Security

### JWT Authentication

The application uses JWT tokens for stateless authentication:

1. User logs in with username/password
2. Server validates credentials and returns a JWT token
3. Client includes the token in `Authorization: Bearer <token>` header for subsequent requests
4. Server validates the token for each protected endpoint

### Token Blacklisting

When a user logs out, the token is added to a Redis-based blacklist to prevent reuse.

### Password Security

User passwords are hashed using BCrypt before storage.

## Caching Strategy

The application uses Redis for multiple caching layers:

1. **Crawl State Cache** (`crawlState`): In-progress crawl states
2. **Final Crawl State Cache** (`crawlStateFinal`): Completed crawl results
3. **Token Blacklist**: Revoked JWT tokens

## Concurrency Model

The web crawler uses:

- **ConcurrentHashMap**: Thread-safe storage of active crawl states
- **ExecutorService**: Concurrent processing of crawl jobs
- **Callback Pattern**: `onStateDone()` callback for crawl completion

## Development

### Running Tests

```
bash ./mvnw test
``` 

### Building for Production

```
bash ./mvnw clean 
```

The JAR file will be created in `target/` directory.

### Docker Build (Optional)

You can containerize the application:

```
dockerfile FROM eclipse-temurin:25-jdk-alpine WORKDIR /app COPY target/*.jar app.jar EXPOSE 8080 ENTRYPOINT ["java", "-jar", "app.jar"]
``` 

Build and run:

```bash
docker build -t java-concurrent-web-crawler .
docker run -p 8080:8080 --env-file .env java-concurrent-web-crawler
```

## Monitoring and Health Checks
Spring Boot Actuator endpoints (if enabled):
- ```/actuator/health``` - Application health status
- ```/actuator/info``` - Application information
- ```/actuator/metrics``` - Application metrics

## License
This project is licensed under the MIT License - see the LICENSE file for details.
