package com.url.shortner

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [FlywayAutoConfiguration::class])
class UrlShortnerApplication

fun main(args: Array<String>) {
    runApplication<UrlShortnerApplication>(*args)
}
