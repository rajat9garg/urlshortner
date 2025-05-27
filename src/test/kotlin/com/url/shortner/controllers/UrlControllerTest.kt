package com.url.shortner.controllers

import com.url.shortner.services.RedirectService
import com.url.shortner.services.UrlService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(UrlController::class)
class UrlControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var urlService: UrlService

    @MockBean
    lateinit var redirectService: RedirectService

    @Test
    fun `redirects to long url when found`() {
        Mockito.`when`(redirectService.getLongUrl("abc123")).thenReturn("https://example.com")
        mockMvc.get("/v1/abc123")
            .andExpect {
                status { isFound() }
                header { string(HttpHeaders.LOCATION, "https://example.com") }
            }
    }

    @Test
    fun `returns 404 when short url not found`() {
        Mockito.`when`(redirectService.getLongUrl("notfound")).thenReturn(null)
        mockMvc.get("/v1/notfound")
            .andExpect {
                status { isNotFound() }
            }
    }
}
