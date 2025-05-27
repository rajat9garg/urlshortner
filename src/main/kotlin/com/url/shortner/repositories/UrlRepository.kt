package com.url.shortner.repositories

import com.url.shortner.models.Urls
import java.time.LocalDateTime

interface UrlRepository {
    fun save(shortCode: String, originalUrl: String, expiresAt: LocalDateTime?): Urls
    fun findByShortCode(shortCode: String): Urls?
    fun existsByShortCode(shortCode: String): Boolean
    fun incrementClickCount(shortCode: String): Int
    fun findByOriginalUrl(originalUrl: String): Urls?
}
