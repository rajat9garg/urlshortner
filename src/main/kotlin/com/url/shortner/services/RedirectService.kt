package com.url.shortner.services

import com.url.shortner.repositories.UrlRepository
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value
import java.time.LocalDateTime

@Service
class RedirectService(
    private val urlCacheService: UrlCacheService,
    private val urlRepository: UrlRepository,
    @Value("\${url.cache.ttl:86400}") private val cacheTtl: Long
) {
    /**
     * Returns the original long URL for the given short code.
     * Looks up Redis cache first, then falls back to DB.
     * Updates cache on DB hit. Returns null if not found or expired.
     */
    fun getLongUrl(shortUrl: String): String? {
        // 1. Try Redis cache
        val cached = urlCacheService.getOriginalUrl(shortUrl)
        if (cached != null) {
            return cached
        }
        // 2. Fallback to DB
        val urlEntity = urlRepository.findByShortCode(shortUrl)
        // 3. Check if found and active, not expired
        if (urlEntity != null && urlEntity.isActive && !isExpired(urlEntity.expiresAt)) {
            // 4. Update cache
            urlCacheService.cacheShortCode(shortUrl, urlEntity.originalUrl, cacheTtl)
            return urlEntity.originalUrl
        }
        // 5. Not found or expired
        return null
    }

    private fun isExpired(expiresAt: LocalDateTime?): Boolean {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now())
    }
}
