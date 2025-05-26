package com.url.shortner.services

import com.url.shortner.models.Urls
import java.time.LocalDateTime

interface UrlService {
    fun createShortUrl(originalUrl: String, customAlias: String? = null, expiresAt: LocalDateTime? = null): Urls
    fun getOriginalUrl(shortCode: String): Urls?
    fun incrementClickCount(shortCode: String)
}
