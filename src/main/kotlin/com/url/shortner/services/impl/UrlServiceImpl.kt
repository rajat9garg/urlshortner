package com.url.shortner.services.impl

import com.url.shortner.models.Urls
import com.url.shortner.repositories.UrlRepository
import com.url.shortner.services.UrlCacheService
import com.url.shortner.services.UrlService
import com.url.shortner.util.Base62
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.LocalDateTime

@Service
class UrlServiceImpl(
    private val urlRepository: UrlRepository,
    private val urlCacheService: UrlCacheService,
    private val redisTemplate: StringRedisTemplate
) : UrlService {
    override fun createShortUrl(originalUrl: String, customAlias: String?, expiresAt: LocalDateTime?): Urls {
        val shortCode = customAlias ?: generateShortCode(originalUrl)
        checkForAlreadyExistingUrl(originalUrl)?.let { return it }

        val url = urlRepository.save(shortCode, originalUrl, expiresAt)
        urlCacheService.cacheShortCode(shortCode, originalUrl, ttlSeconds = expiresAt?.let { java.time.Duration.between(LocalDateTime.now(), it).seconds })
        return url
    }

    override fun getOriginalUrl(shortCode: String): Urls? {
        val cached = urlCacheService.getOriginalUrl(shortCode)
        if (cached != null) {
            return Urls(
                id = -1, // Indicate cache hit, not from DB
                shortCode = shortCode,
                originalUrl = cached,
                createdAt = LocalDateTime.now(),
                expiresAt = null,
                isActive = true,
                totalClicks = 0
            )
        }
        val url = urlRepository.findByShortCode(shortCode)
        if (url != null) {
            urlCacheService.cacheShortCode(shortCode, url.originalUrl, ttlSeconds = url.expiresAt?.let { java.time.Duration.between(LocalDateTime.now(), it).seconds })
        }
        return url
    }

    override fun incrementClickCount(shortCode: String) {
        urlRepository.incrementClickCount(shortCode)
    }

    private fun checkForAlreadyExistingUrl(originalUrl: String): Urls? {
        return urlRepository.findByOriginalUrl(originalUrl)
    }

    private fun generateShortCode(originalUrl: String): String {
        // 1. Increment a global Redis counter for uniqueness
        val counter = redisTemplate.opsForValue().increment("url:counter") ?: System.currentTimeMillis()
        // 2. Hash the original URL for additional uniqueness
        val hash = MessageDigest.getInstance("SHA-256").digest(originalUrl.toByteArray())
        val hashLong = hash.take(8).fold(0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 0xff) }
        // 3. Combine counter and hash
        val combined = (counter shl 32) or (hashLong and 0xFFFFFFFFL)
        // 4. Encode as Base62 and take first 8 chars
        return Base62.encode(combined).padStart(8, '0').take(8)
    }
}
