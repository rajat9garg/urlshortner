package com.url.shortner.controllers


import com.url.shortener.generated.api.HealthApi
import com.url.shortener.generated.model.HealthResponse
import com.url.shortner.mappers.HealthMapper
import com.url.shortner.services.HealthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Health", description = "Health Check Endpoints")
class HealthController(
    private val healthService: HealthService,
    private val healthMapper: HealthMapper
): HealthApi {


    override fun healthCheck(): ResponseEntity<HealthResponse> {
        val status = healthService.checkHealth()
        val response = healthMapper.toHealthResponse(status)
        return ResponseEntity.ok(
            HealthResponse(
                status = response.status
            )
        )
    }
}
