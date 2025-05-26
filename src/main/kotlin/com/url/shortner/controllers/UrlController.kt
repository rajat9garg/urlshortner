package com.url.shortner.controllers

import com.url.shortner.services.UrlService
import com.url.shortener.generated.api.URLsApi
import com.url.shortener.generated.model.CreateUrlRequest
import com.url.shortener.generated.model.UrlResponse
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
    private val urlService: UrlService
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
                    shortUrl = "http://localhost:8080/urls/${result.shortCode}",
                    originalUrl = result.originalUrl,
                    expiresAt = result.expiresAt?.let { OffsetDateTime.of(it, ZoneOffset.UTC) }
            ))
    }
}
