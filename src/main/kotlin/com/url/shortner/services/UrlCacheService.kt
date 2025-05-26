package com.url.shortner.services

import com.url.shortner.config.RedisService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class UrlCacheService(
    private val redisService: RedisService,
    @Value("\${url.cache.ttl:3600}") private val defaultTtl: Long
) {
    fun cacheShortCode(shortCode: String, originalUrl: String, ttlSeconds: Long? = null) {
        redisService.set(shortCode, originalUrl, ttlSeconds ?: defaultTtl)
    }

    fun getOriginalUrl(shortCode: String): String? = redisService.get(shortCode)

    fun evictShortCode(shortCode: String) {
        redisService.delete(shortCode)
    }
}
