Functional Requirements
1. Users should be able to generate short URLs for long URLs.
2. Users should be able to retrieve the original long URL from a short URL.

Non Functional Requirements
1. The system should make sure all the generated URls are unique.
2. The system should make sure all the redirections happens < 100ms
3. The system should be able to handle 1B urls.

Estimations
1. Create Url Request QPS: 10K/S
2. Redirect Request QPS: 100K/S

Core Entities
1. Users
2. URLs (short and long)

Assumptions
1. The users are already created and authenticated in the system and no need to be authenticated again.

Apis
1. POST /v1/create
   {
    "longUrl": "https://www.google.com"
    "userID": String
    "Expiry": Timestamp 
    }

    "200":{
            shortUrl: String
            }

2. Redirection API
   GET /v1/{shortUrl}
   302: {longUrl: String}

Url Generation Flow
1. Use Redis Counter and base64 encoding of the longUrl to generate shortcode
2. Every shortUrl will be http://localhost:8080/v1/{shortCode}
3. Store Every shortUrl to longUrl in Redis with expiry of 1 day
4. Store mapping of shortUrl to longUrl in DB
5. Use MongoDB to store mapping of shortUrl to longUrl

Redirection Flow
1. Get shortUrl from redis
2. Get longUrl from DB
3. Redirect to longUrl

Tasks1
- Implement and Register New Table for URLs
- Implement Redis Connection
- Implement Endpoints for creating short URLs

Tasks2
- Implement logic to redirect to long URLS when short URL is hit
- Use Local Host