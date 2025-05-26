# Task 1 Implementation Plan: URL Shortener Service

## Metadata Header
- **Created:** 2025-05-26
- **Author:** Coding Agent
- **Status:** [DRAFT]
- **Last Modified:** 2025-05-26

---

## Objective
Implement and register the new URLs table, establish Redis connection, and develop endpoints for creating short URLs as per requirements.

---

## 1. Database Design: URLs Table

### Table Schema
| Column        | Type         | Constraints              |
|--------------|--------------|--------------------------|
| id           | BIGSERIAL    | PRIMARY KEY              |
| short_code   | VARCHAR(10)  | UNIQUE, NOT NULL         |
| original_url | TEXT         | NOT NULL                 |
| created_at   | TIMESTAMPTZ  | DEFAULT NOW(), NOT NULL  |
| expires_at   | TIMESTAMPTZ  | NULLABLE                 |
| is_active    | BOOLEAN      | DEFAULT TRUE, NOT NULL   |
| total_clicks | BIGINT       | DEFAULT 0, NOT NULL      |

### Flyway Migration
- Create migration file: `V2__create_urls_table.sql`
- Include table creation, indexes, and trigger for `updated_at`
- Follow existing migration pattern from `V1__create_users_table.sql`

### jOOQ Configuration
- Use jOOQ for type-safe SQL queries
- Generate jOOQ classes from database schema
- Configure jOOQ in Gradle build script

### Design Pattern: Repository Pattern
- Implement UrlRepository using jOOQ for database operations
- Provide methods for CRUD operations on URLs table

---

## 2. Redis Connection

### Purpose
- Cache short_code → original_url mappings for fast lookup
- Reduce DB load on redirection

### Redis Setup
- Use Spring Data Redis
- Configure Redis connection in `application.yml`:
  ```yaml
  spring:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 2000
  ```
- Use StringRedisTemplate for string operations
- TTL (Time-to-Live): Set cache expiry to match URL expiration if set; otherwise, use a default (e.g., 24h)

### Cache Implementation
```kotlin
@Service
class UrlCacheService(private val redisTemplate: StringRedisTemplate) {
    
    fun cacheUrl(shortCode: String, originalUrl: String, expiresAt: LocalDateTime?) {
        val operations = redisTemplate.opsForValue()
        if (expiresAt != null) {
            val ttl = ChronoUnit.SECONDS.between(LocalDateTime.now(), expiresAt)
            operations.set(shortCode, originalUrl, Duration.ofSeconds(ttl))
        } else {
            operations.set(shortCode, originalUrl, Duration.ofHours(24))
        }
    }
    
    fun getOriginalUrl(shortCode: String): String? {
        return redisTemplate.opsForValue().get(shortCode)
    }
}
```

---

## 3. OpenAPI Specification

### Update api.yaml
- Add URL shortening endpoints following OpenAPI 3.0.3 specification
- Store in `src/main/resources/openapi/api.yaml`
- Follow naming conventions from memory bank (snake_case for properties, PascalCase for schemas)

```yaml
openapi: 3.0.3
info:
  title: URL Shortener API
  description: API for shortening URLs
  version: 1.0.0
servers:
  - url: /api/v1
    description: Production server
paths:
  /urls:
    post:
      summary: Create short URL
      description: Creates a shortened URL from the original URL
      operationId: createShortUrl
      tags:
        - URLs
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateUrlRequest'
      responses:
        '201':
          description: Short URL created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UrlResponse'
        '400':
          description: Invalid input
        '409':
          description: Custom alias already exists
components:
  schemas:
    CreateUrlRequest:
      type: object
      required:
        - original_url
      properties:
        original_url:
          type: string
          format: uri
          example: "https://www.example.com/very/long/url/that/needs/shortening"
          description: The original URL to be shortened
        custom_alias:
          type: string
          example: "custom"
          description: Optional custom alias for the shortened URL
        expires_at:
          type: string
          format: date-time
          example: "2025-06-26T00:00:00Z"
          description: Optional expiration date and time for the shortened URL
    UrlResponse:
      type: object
      properties:
        short_url:
          type: string
          example: "http://localhost:8080/abc123"
          description: The shortened URL
        original_url:
          type: string
          example: "https://www.example.com/very/long/url/that/needs/shortening"
          description: The original URL
        expires_at:
          type: string
          format: date-time
          example: "2025-06-26T00:00:00Z"
          description: Expiration date and time for the shortened URL
```


## 4. Controller Implementation

### UrlController
```kotlin
@RestController
@Tag(name = "URLs", description = "URL Shortening Endpoints")
class UrlController(
    private val urlService: UrlService
) : UrlsApi {

    override fun createShortUrl(
        @Valid createUrlRequest: CreateUrlRequest
    ): ResponseEntity<UrlResponse> {
        val result = urlService.createShortUrl(
            originalUrl = createUrlRequest.originalUrl,
            customAlias = createUrlRequest.customAlias,
            expiresAt = createUrlRequest.expiresAt?.toLocalDateTime()
        )
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(result)
    }
}
```

---

## 5. Service Implementation

### UrlService
```kotlin
interface UrlService {
    fun createShortUrl(
        originalUrl: String,
        customAlias: String? = null,
        expiresAt: LocalDateTime? = null
    ): UrlResponse
}
```

### UrlServiceImpl
```kotlin
@Service
class UrlServiceImpl(
    private val urlRepository: UrlRepository,
    private val urlCacheService: UrlCacheService,
    @Value("\${app.base-url}") private val baseUrl: String
) : UrlService {

    override fun createShortUrl(
        originalUrl: String,
        customAlias: String?,
        expiresAt: LocalDateTime?
    ): UrlResponse {
        // Validate URL
        validateUrl(originalUrl)
        
        // Generate or validate short code
        val shortCode = customAlias ?: generateShortCode()
        
        // Check if custom alias is already taken
        if (customAlias != null && urlRepository.existsByShortCode(shortCode)) {
            throw CustomAliasAlreadyExistsException("Custom alias '$customAlias' is already in use")
        }
        
        // Save to database
        val url = urlRepository.save(
            shortCode = shortCode,
            originalUrl = originalUrl,
            expiresAt = expiresAt
        )
        
        // Cache in Redis
        urlCacheService.cacheUrl(shortCode, originalUrl, expiresAt)
        
        // Return response
        return UrlResponse(
            shortUrl = "$baseUrl/$shortCode",
            originalUrl = originalUrl,
            expiresAt = expiresAt?.toString()
        )
    }
    
    private fun validateUrl(url: String) {
        try {
            URL(url)
        } catch (e: Exception) {
            throw InvalidUrlException("Invalid URL format: $url")
        }
    }
    
    private fun generateShortCode(): String {
        // Base62 encoding using random bytes
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        val length = 6
        
        for (attempt in 1..5) {
            val shortCode = (1..length)
                .map { allowedChars[random.nextInt(allowedChars.length)] }
                .joinToString("")
                
            if (!urlRepository.existsByShortCode(shortCode)) {
                return shortCode
            }
        }
        
        throw RuntimeException("Failed to generate unique short code after 5 attempts")
    }
}
```

---

## 6. Repository Implementation with jOOQ

### UrlRepository
```kotlin
interface UrlRepository {
    fun save(shortCode: String, originalUrl: String, expiresAt: LocalDateTime?): Url
    fun findByShortCode(shortCode: String): Url?
    fun existsByShortCode(shortCode: String): Boolean
    fun incrementClickCount(shortCode: String): Int
}
```

### UrlRepositoryImpl
```kotlin
@Repository
class UrlRepositoryImpl(private val dsl: DSLContext) : UrlRepository {

    override fun save(shortCode: String, originalUrl: String, expiresAt: LocalDateTime?): Url {
        val record = dsl.insertInto(URLS)
            .set(URLS.SHORT_CODE, shortCode)
            .set(URLS.ORIGINAL_URL, originalUrl)
            .set(URLS.EXPIRES_AT, expiresAt?.toOffsetDateTime())
            .set(URLS.IS_ACTIVE, true)
            .set(URLS.TOTAL_CLICKS, 0L)
            .returning()
            .fetchOne() ?: throw RuntimeException("Failed to insert URL")
            
        return Url(
            id = record.id,
            shortCode = record.shortCode,
            originalUrl = record.originalUrl,
            createdAt = record.createdAt.toLocalDateTime(),
            expiresAt = record.expiresAt?.toLocalDateTime(),
            isActive = record.isActive,
            totalClicks = record.totalClicks
        )
    }
    
    override fun findByShortCode(shortCode: String): Url? {
        return dsl.selectFrom(URLS)
            .where(URLS.SHORT_CODE.eq(shortCode))
            .and(URLS.IS_ACTIVE.eq(true))
            .fetchOne()
            ?.let {
                Url(
                    id = it.id,
                    shortCode = it.shortCode,
                    originalUrl = it.originalUrl,
                    createdAt = it.createdAt.toLocalDateTime(),
                    expiresAt = it.expiresAt?.toLocalDateTime(),
                    isActive = it.isActive,
                    totalClicks = it.totalClicks
                )
            }
    }
    
    override fun existsByShortCode(shortCode: String): Boolean {
        return dsl.fetchExists(
            dsl.selectOne()
                .from(URLS)
                .where(URLS.SHORT_CODE.eq(shortCode))
        )
    }
    
    override fun incrementClickCount(shortCode: String): Int {
        return dsl.update(URLS)
            .set(URLS.TOTAL_CLICKS, URLS.TOTAL_CLICKS.plus(1))
            .where(URLS.SHORT_CODE.eq(shortCode))
            .execute()
    }
}
```

---

## 7. Exception Handling

### Custom Exceptions
```kotlin
class InvalidUrlException(message: String) : RuntimeException(message)
class CustomAliasAlreadyExistsException(message: String) : RuntimeException(message)
class UrlNotFoundException(message: String) : RuntimeException(message)
```

### Global Exception Handler
```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(InvalidUrlException::class)
    fun handleInvalidUrlException(ex: InvalidUrlException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = ex.message))
    }
    
    @ExceptionHandler(CustomAliasAlreadyExistsException::class)
    fun handleCustomAliasAlreadyExistsException(ex: CustomAliasAlreadyExistsException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse(message = ex.message))
    }
    
    @ExceptionHandler(UrlNotFoundException::class)
    fun handleUrlNotFoundException(ex: UrlNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(message = ex.message))
    }
}
```

---

## 8. Implementation Checklist

- [ ] Create Flyway migration for URLs table
- [ ] Configure jOOQ in build.gradle.kts
- [ ] Update OpenAPI specification with URL endpoints
- [ ] Generate API interfaces and models
- [ ] Implement UrlRepository with jOOQ
- [ ] Configure Redis in application.yml
- [ ] Implement UrlCacheService
- [ ] Implement UrlService with short code generation logic
- [ ] Implement UrlController
- [ ] Implement exception handling
- [ ] Write unit/integration tests

---

## 9. URL Shortening Algorithm

According to the requirements document, the URL shortening algorithm should:

1. Use Redis Counter and base64 encoding of the longUrl to generate shortcode
2. Format every shortUrl as `http://localhost:8080/v1/{shortCode}`
3. Store shortUrl to longUrl mapping in Redis with expiry of 1 day
4. Store mapping of shortUrl to longUrl in DB

### Implementation Details

#### Short Code Generation Algorithm

```kotlin
fun generateShortCode(redisTemplate: StringRedisTemplate, longUrl: String): String {
    // 1. Increment Redis counter to get unique ID
    val counter = redisTemplate.opsForValue().increment("url:counter") ?: 1L
    
    // 2. Combine counter with longUrl hash for uniqueness
    val urlHash = longUrl.hashCode()
    val combined = "$counter:$urlHash"
    
    // 3. Encode using Base64 and make URL-safe
    val encoded = Base64.getEncoder().encodeToString(combined.toByteArray())
        .replace('+', '-')
        .replace('/', '_')
        .replace("=", "")
    
    // 4. Truncate to desired length (6 chars)
    return encoded.take(6)
}
```

This algorithm ensures:
- Uniqueness through Redis counter (atomic increment)
- Additional uniqueness by incorporating the URL hash
- URL-safe encoding by replacing Base64 special characters
- Consistent length for all short codes

### Redis Caching Strategy

As per requirements:
1. Cache every shortUrl → longUrl mapping in Redis
2. Set TTL (Time-to-Live) to 1 day

```kotlin
fun cacheUrlMapping(redisTemplate: StringRedisTemplate, shortCode: String, longUrl: String) {
    // Set the mapping with 1 day expiry
    redisTemplate.opsForValue().set(
        "url:$shortCode",
        longUrl,
        Duration.ofDays(1)
    )
}
```

### Database Storage

The URLs table will store the permanent mapping between short codes and original URLs:

```sql
CREATE TABLE urls (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL UNIQUE,
    original_url TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    total_clicks BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_urls_short_code ON urls(short_code);
```

### URL Lookup Flow

1. First check Redis cache for the short code
2. If not found, query the database
3. If found, increment click count and return the original URL
4. If the URL has expired, return a 404 Not Found

### Performance Considerations

- Redis caching ensures redirect operations meet the <100ms requirement
- The system can handle 1B URLs with the BIGSERIAL primary key
- The algorithm can support the estimated QPS:
  - 10K/s for URL creation
  - 100K/s for redirections (primarily served from Redis)

---

## References
- [techContext.md](../memory-bank/techContext.md)
- [systemPatterns.md](../memory-bank/systemPatterns.md)
- [requirements.md](./requirements.md)
