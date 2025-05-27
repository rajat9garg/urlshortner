package com.url.shortner.controllers

import com.url.shortener.generated.api.URLsApi
import com.url.shortener.generated.model.CreateUrlRequest
import com.url.shortener.generated.model.UrlResponse
import com.url.shortner.services.RedirectService
import com.url.shortener.generated.model.CreateUrlRequest as GeneratedCreateUrlRequest
import com.url.shortener.generated.model.UrlResponse as GeneratedUrlResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset

@RestController
class UrlController(
    private val urlService: UrlService,
    private val redirectService: RedirectService
) : URLsApi {

    override fun createShortUrl(createUrlRequest: CreateUrlRequest): ResponseEntity<UrlResponse> {
        val result = urlService.createShortUrl(
            originalUrl = createUrlRequest.originalUrl.toString(),
            customAlias = createUrlRequest.customAlias,
            expiresAt = createUrlRequest.expiresAt?.toLocalDateTime()
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                UrlResponse(
                    shortUrl = "http://localhost:8080/api/v1/${result.shortCode}",
                    originalUrl = result.originalUrl,
                    expiresAt = result.expiresAt?.let { OffsetDateTime.of(it, ZoneOffset.UTC) }
            ))
    }

    override fun redirectToLongUrl(shortUrl: String): ResponseEntity<Unit> {
        val longUrl = redirectService.getLongUrl(shortUrl)
        return if (longUrl != null) {
            ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build()
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }
}
