package com.url.shortner.mappers

import com.url.shortner.models.dto.HealthResponse
import org.springframework.stereotype.Component

@Component
class HealthMapper {
    fun toHealthResponse(status: String): HealthResponse {
        return HealthResponse(status = status)
    }
}
