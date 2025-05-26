package com.url.shortner.services.impl

import com.url.shortner.models.Urls
import com.url.shortner.repositories.UrlRepository
import com.url.shortner.services.UrlCacheService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.LocalDateTime
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify

class UrlServiceImplTest {
    private lateinit var urlRepository: UrlRepository
    private lateinit var urlCacheService: UrlCacheService
    private lateinit var redisTemplate: StringRedisTemplate
    private lateinit var urlService: UrlServiceImpl

    @BeforeEach
    fun setUp() {
        urlRepository = mockk()
        urlCacheService = mockk()
        redisTemplate = mockk()
        urlService = UrlServiceImpl(urlRepository, urlCacheService, redisTemplate)
    }

    @Test
    fun `should create short url without custom alias`() {
        val originalUrl = "https://example.com"
        val expiresAt = LocalDateTime.now().plusDays(1)
        every { redisTemplate.opsForValue().increment(any()) } returns 1L
        every { urlRepository.save(any(), any(), any()) } returns Urls(1, "abc12345", originalUrl, LocalDateTime.now(), expiresAt, true, 0)
        every { urlCacheService.cacheShortCode(any(), any(), any()) } returns Unit

        val result = urlService.createShortUrl(originalUrl, null, expiresAt)
        assertEquals(originalUrl, result.originalUrl)
        assertEquals(expiresAt, result.expiresAt)
        assertTrue(result.shortCode.isNotBlank())
    }

    @Test
    fun `should create short url with custom alias`() {
        val originalUrl = "https://example.com"
        val customAlias = "myalias"
        val expiresAt = LocalDateTime.now().plusDays(1)
        every { urlRepository.save(customAlias, originalUrl, expiresAt) } returns Urls(2, customAlias, originalUrl, LocalDateTime.now(), expiresAt, true, 0)
        every { urlCacheService.cacheShortCode(any(), any(), any()) } returns Unit

        val result = urlService.createShortUrl(originalUrl, customAlias, expiresAt)
        assertEquals(customAlias, result.shortCode)
        assertEquals(originalUrl, result.originalUrl)
    }

    @Test
    fun `should get original url from cache`() {
        val shortCode = "abc12345"
        val cachedUrl = "https://cached.com"
        every { urlCacheService.getOriginalUrl(shortCode) } returns cachedUrl

        val result = urlService.getOriginalUrl(shortCode)
        assertNotNull(result)
        assertEquals(cachedUrl, result!!.originalUrl)
    }

    @Test
    fun `should get original url from db and cache it`() {
        val shortCode = "abc12345"
        val dbUrl = Urls(3, shortCode, "https://fromdb.com", LocalDateTime.now(), null, true, 0)
        every { urlCacheService.getOriginalUrl(shortCode) } returns null
        every { urlRepository.findByShortCode(shortCode) } returns dbUrl
        every { urlCacheService.cacheShortCode(shortCode, dbUrl.originalUrl, any()) } returns Unit

        val result = urlService.getOriginalUrl(shortCode)
        assertNotNull(result)
        assertEquals(dbUrl.originalUrl, result!!.originalUrl)
    }

    @Test
    fun `should return null if short code does not exist`() {
        val shortCode = "notfound"
        every { urlCacheService.getOriginalUrl(shortCode) } returns null
        every { urlRepository.findByShortCode(shortCode) } returns null

        val result = urlService.getOriginalUrl(shortCode)
        assertNull(result)
    }

    @Test
    fun `should increment click count`() {
        val shortCode = "abc12345"
        every { urlRepository.incrementClickCount(shortCode) } returns 1

        urlService.incrementClickCount(shortCode)
        verify { urlRepository.incrementClickCount(shortCode) }
    }
}
