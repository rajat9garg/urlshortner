# Task 2 Plan: Implement Redirection Logic

## Objective
Implement the logic to redirect users to the original long URL when a short URL is accessed, ensuring high performance and reliability.

## Steps
1. **API Implementation**
   - Implement the GET `/v1/{shortUrl}` endpoint.
   - On request, extract `{shortUrl}` as the short code.

2. **Cache Lookup**
   - Check Redis cache for the mapping of short code to long URL.
   - If found, proceed to redirect.

3. **Database Fallback**
   - If not found in Redis, query the database (MongoDB) for the mapping.
   - If found, update Redis cache with the mapping (with a 1-day TTL).

4. **Redirection**
   - If mapping exists, return HTTP 302 with the long URL in the `Location` header.
   - If not found, return HTTP 404 with an error message.

5. **Performance**
   - Ensure response time is <100ms for redirection.
   - Add basic logging and error handling.

6. **Testing**
   - Write unit and integration tests for the redirection flow.
   - Test cache hit, cache miss, and not-found scenarios.

## Controller & Service Implementation Details

### Controller
- Exposes the GET `/v1/{shortUrl}` endpoint.
- Accepts the short URL code as a path variable.
- Validates input format and length.
- Delegates business logic to the service layer.
- Handles and maps exceptions to HTTP responses (e.g., 404, 500).
- Sets HTTP 302 status and `Location` header for redirection.
- Logs incoming requests and outcomes.

### Service
- Encapsulates all business logic for URL resolution and redirection.
- Accepts the short code from the controller.
- Checks Redis cache for the long URL mapping.
- If not found, queries MongoDB for the mapping.
- On DB hit, updates Redis cache with a 1-day TTL.
- Returns the long URL to the controller if found.
- Throws a specific exception if mapping is not found.
- Logs cache hits, misses, and errors.
- Ensures synchronous and performant operations (<100ms).

## OpenAPI Implementation Details

- The OpenAPI spec for the redirection endpoint will be defined in `src/main/resources/openapi/api.yaml`.
- Endpoint: `GET /v1/{shortUrl}`
  - Path parameter: `shortUrl` (string, required)
- Responses:
  - `302 Found`: Redirects to the original long URL via the `Location` header, with an example.
  - `404 Not Found`: Returned if the short URL does not exist, with an error schema and example.
- All schemas will be defined in the `schemas/` directory and referenced via `$ref`.
- The endpoint will be documented with clear descriptions, request/response examples, and all required fields marked.
- No authentication or security schemes will be defined for this endpoint.
- API versioning will be present in the path (`/v1/`).
- Only health and redirection endpoints will be included in the spec, per user rules.

## Redirection Flow Diagram

```mermaid
flowchart TD
    A[User requests short URL: /v1/{shortUrl}] --> B{Check Redis Cache}
    B -- Found --> C[Redirect (HTTP 302) to long URL]
    B -- Not Found --> D[Query MongoDB for mapping]
    D -- Found --> E[Update Redis Cache (1-day TTL)]
    E --> C
    D -- Not Found --> F[Return HTTP 404 Not Found]
```

## Notes
- Use localhost for all endpoints.
- Follow layered architecture: Controller → Service → Repository/Cache.
- No authentication required for redirection.
- Ensure input validation and proper error responses.
