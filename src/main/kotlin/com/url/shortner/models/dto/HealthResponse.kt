package com.url.shortner.models.dto

import java.time.Instant

data class HealthResponse(
    val status: String,
    val timestamp: Instant = Instant.now()
)
