package com.url.shortner.services.impl

import com.url.shortner.services.HealthService
import org.springframework.stereotype.Service

@Service
class HealthServiceImpl : HealthService {
    override fun checkHealth(): String {
        return "UP"
    }
}
