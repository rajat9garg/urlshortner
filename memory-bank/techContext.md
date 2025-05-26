## Metadata Header
- **Created:** 2025-05-26
- **Author:** Rajat Garg
- **Status:** [DRAFT]
- **Last Modified:** 2025-05-26

# Technical Context: URL Shortener Service

## 🛠 Technology Stack

### Core Technologies
- **Language**: Kotlin 1.9.0
- **Framework**: Spring Boot 3.1.0
- **Build Tool**: Gradle 8.0+
- **JVM Version**: 17 (Temurin)

### Database
- **Primary**: PostgreSQL 15
  - **Extensions**:
    - `pgcrypto` for encryption
    - `uuid-ossp` for UUID generation
    - `pg_stat_statements` for query monitoring
- **Migrations**: Flyway
- **Connection Pool**: HikariCP

### Caching
- **Primary**: Redis 7.0
  - **Use Cases**:
    - URL lookups
    - Rate limiting
    - Session storage

### Search (Future)
- **Technology**: Elasticsearch 8.7
  - **Use Cases**:
    - Analytics queries
    - URL search

## 🏗 System Architecture

### High-Level Architecture
```
┌─────────────────────────────────────────────────────────────────┐
│                       Client Applications                       │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                         API Gateway                            │
│  ┌─────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │  Auth       │  │  Rate Limiting  │  │  Request        │  │
│  │  Service    │  │  Service        │  │  Validation     │  │
│  └─────────────┘  └─────────────────┘  └─────────────────┘  │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Application Services                      │
│  ┌─────────────────┐  ┌─────────────────┐  ┌───────────────┐  │
│  │  URL Service   │  │  Analytics      │  │  User         │  │
│  │                 │  │  Service        │  │  Service      │  │
│  └─────────────────┘  └─────────────────┘  └───────────────┘  │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Data Layer                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌───────────────┐  │
│  │  PostgreSQL    │  │  Redis          │  │  File Storage │  │
│  │  (Primary DB)  │  │  (Cache)        │  │  (Logs, etc)  │  │
│  └─────────────────┘  └─────────────────┘  └───────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Service Components

#### 1. API Gateway
- **Technology**: Spring Cloud Gateway
- **Features**:
  - Request routing
  - Authentication/Authorization
  - Rate limiting
  - Request/response transformation
  - Circuit breaking

#### 2. URL Service
- **Responsibilities**:
  - URL shortening
  - URL validation
  - Expiration handling
  - Custom alias management
- **Key Classes**:
  - `URLController`
  - `URLService`
  - `URLRepository`
  - `URLGenerator`

#### 3. Analytics Service
- **Responsibilities**:
  - Click tracking
  - Geographic analysis
  - Device/browser tracking
  - Reporting
- **Key Classes**:
  - `AnalyticsController`
  - `AnalyticsService`
  - `ClickRepository`

## 🗃️ Database Schema

### Core Tables

#### 1. urls
```sql
CREATE TABLE urls (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL UNIQUE,
    original_url TEXT NOT NULL,
    user_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT true,
    total_clicks BIGINT NOT NULL DEFAULT 0,
    last_clicked_at TIMESTAMPTZ,
    metadata JSONB
);

-- Indexes
CREATE INDEX idx_urls_short_code ON urls(short_code);
CREATE INDEX idx_urls_user_id ON urls(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_urls_expires_at ON urls(expires_at) WHERE expires_at IS NOT NULL;
```

#### 2. clicks
```sql
CREATE TABLE clicks (
    id BIGSERIAL PRIMARY KEY,
    url_id BIGINT NOT NULL REFERENCES urls(id) ON DELETE CASCADE,
    clicked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    referrer TEXT,
    user_agent TEXT,
    ip_address INET,
    country_code CHAR(2),
    device_type VARCHAR(20),
    os VARCHAR(50),
    browser VARCHAR(50)
);

-- Indexes
CREATE INDEX idx_clicks_url_id ON clicks(url_id);
CREATE INDEX idx_clicks_clicked_at ON clicks(clicked_at);
```

## 🚀 Getting Started

### Prerequisites
- Java 17 JDK
- Docker 20.10+
- Docker Compose 2.0+
- Git

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/url-shortener.git
   cd url-shortener
   ```

2. **Set up environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

3. **Start dependencies**
   ```bash
   docker-compose up -d postgres redis
   ```

4. **Run database migrations**
   ```bash
   ./gradlew flywayMigrate
   ```

5. **Start the application**
   ```bash
   ./gradlew bootRun
   ```

6. **Verify the application**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

### API Documentation

Once the application is running, access the API documentation at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## 🔧 Configuration

### Application Properties
```yaml
# Server Configuration
server:
  port: 8080
  servlet:
    context-path: /

# Database Configuration
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/urlshortener
    username: postgres
    password: postgres
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000

  # JPA/Hibernate
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        jdbc:
          batch_size: 20

# Redis Configuration
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.timeout=5000

# Application Properties
app:
  base-url: http://localhost:8080
  url:
    short-code-length: 6
    default-expiration-days: 365
  rate-limit:
    enabled: true
    limit: 100
    duration: 1m

# Logging
logging:
  level:
    root: INFO
    com.example.urlshortener: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

## 🧪 Testing

### Running Tests
```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew integrationTest

# All tests with coverage
./gradlew check jacocoTestReport
```

### Test Containers
Integration tests use TestContainers for:
- PostgreSQL
- Redis
- WireMock (for external services)

## 🚢 Deployment

### Building the Application
```bash
# Build JAR
./gradlew bootJar

# Build Docker image
docker build -t url-shortener:latest .
```

### Docker Compose (Production)
```yaml
version: '3.8'

services:
  app:
    image: url-shortener:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/urlshortener
      - SPRING_DATASOURCE_USERNAME=${DB_USER}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - SPRING_REDIS_HOST=redis
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - redis
    restart: unless-stopped

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=urlshortener
      - POSTGRES_USER=${DB_USER}
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    restart: unless-stopped

volumes:
  postgres_data:
  redis_data:
```

## 📊 Monitoring & Operations

### Health Endpoints
- `/actuator/health`: Application health
- `/actuator/info`: Application info
- `/actuator/metrics`: Application metrics
- `/actuator/prometheus`: Prometheus metrics

### Logging
- Structured JSON logging
- Correlation IDs for request tracing
- Log levels configurable via environment variables

### Alerting
- Prometheus for metrics collection
- Alertmanager for alerting
- Grafana for visualization

## 🔄 CI/CD Pipeline

### Build & Test
1. Checkout code
2. Set up JDK 17
3. Run unit tests
4. Build application
5. Run integration tests
6. Generate code coverage report
7. Build Docker image
8. Push to container registry

### Deploy
1. Deploy to staging
2. Run smoke tests
3. Manual approval for production
4. Blue-green deployment to production

## 🔗 Related Documents
- [System Patterns](./systemPatterns.md)
- [Project Brief](./projectbrief.md)
- [Active Context](./activeContext.md)
