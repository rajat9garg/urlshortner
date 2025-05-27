package com.url.shortner.services

import com.url.shortner.models.Urls
import com.url.shortner.repositories.UrlRepository
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class RedirectServiceTest {
    private lateinit var urlCacheService: UrlCacheService
    private lateinit var urlRepository: UrlRepository
    private lateinit var redirectService: RedirectService

    @BeforeEach
    fun setUp() {
        urlCacheService = mockk(relaxed = true)
        urlRepository = mockk(relaxed = true)
        redirectService = RedirectService(urlCacheService, urlRepository, 86400)
    }

    @Test
    fun `returns long url from cache if present`() {
        every { urlCacheService.getOriginalUrl("abc123") } returns "https://example.com"
        val result = redirectService.getLongUrl("abc123")
        assertEquals("https://example.com", result)
        verify(exactly = 0) { urlRepository.findByShortCode(any()) }
    }

    @Test
    fun `returns long url from db and updates cache if not in cache`() {
        every { urlCacheService.getOriginalUrl("abc123") } returns null
        val urlEntity = Urls(1, "abc123", "https://db.com", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), true, 0)
        every { urlRepository.findByShortCode("abc123") } returns urlEntity
        every { urlCacheService.cacheShortCode("abc123", "https://db.com", 86400) } returns Unit
        val result = redirectService.getLongUrl("abc123")
        assertEquals("https://db.com", result)
        verify { urlCacheService.cacheShortCode("abc123", "https://db.com", 86400) }
    }

    @Test
    fun `returns null if url is expired`() {
        every { urlCacheService.getOriginalUrl("expired") } returns null
        val urlEntity = Urls(1, "expired", "https://expired.com", LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1), true, 0)
        every { urlRepository.findByShortCode("expired") } returns urlEntity
        val result = redirectService.getLongUrl("expired")
        assertNull(result)
    }

    @Test
    fun `returns null if url is inactive`() {
        every { urlCacheService.getOriginalUrl("inactive") } returns null
        val urlEntity = Urls(1, "inactive", "https://inactive.com", LocalDateTime.now().minusDays(2), LocalDateTime.now().plusDays(1), false, 0)
        every { urlRepository.findByShortCode("inactive") } returns urlEntity
        val result = redirectService.getLongUrl("inactive")
        assertNull(result)
    }

    @Test
    fun `returns null if url not found in cache or db`() {
        every { urlCacheService.getOriginalUrl("notfound") } returns null
        every { urlRepository.findByShortCode("notfound") } returns null
        val result = redirectService.getLongUrl("notfound")
        assertNull(result)
    }
}
