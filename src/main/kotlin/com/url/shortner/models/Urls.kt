package com.url.shortner.models

import java.time.LocalDateTime

data class Urls(
    val id: Long = 0,
    val shortCode: String,
    val originalUrl: String,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime? = null,
    val isActive: Boolean = true,
    val totalClicks: Long = 0
)
