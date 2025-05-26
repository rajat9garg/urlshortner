package com.url.shortner.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RedisService(
    private val redisTemplate: StringRedisTemplate,
    @Value("\${spring.redis.timeout:10000}") private val timeout: Long
) {
    private val valueOps: ValueOperations<String, String> = redisTemplate.opsForValue()

    fun set(key: String, value: String, ttlSeconds: Long? = null) {
        if (ttlSeconds != null) {
            valueOps.set(key, value, Duration.ofSeconds(ttlSeconds))
        } else {
            valueOps.set(key, value)
        }
    }

    fun get(key: String): String? = valueOps.get(key)

    fun delete(key: String) {
        redisTemplate.delete(key)
    }
}
