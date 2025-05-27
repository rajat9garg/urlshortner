package com.url.shortner.repositories

import com.url.shortner.models.Urls
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import com.url.shortner.infrastructure.jooq.tables.Urls as UrlsTable
import com.url.shortner.infrastructure.jooq.tables.records.UrlsRecord

@Repository
class UrlRepositoryImpl(private val dsl: DSLContext) : UrlRepository {
    override fun save(shortCode: String, originalUrl: String, expiresAt: LocalDateTime?): Urls {
        val record = dsl.insertInto(UrlsTable.URLS)
            .set(UrlsTable.URLS.SHORT_CODE, shortCode)
            .set(UrlsTable.URLS.ORIGINAL_URL, originalUrl)
            .set(UrlsTable.URLS.EXPIRES_AT, expiresAt)
            .set(UrlsTable.URLS.IS_ACTIVE, true)
            .set(UrlsTable.URLS.TOTAL_CLICKS, 0L)
            .returning(
                UrlsTable.URLS.ID,
                UrlsTable.URLS.SHORT_CODE,
                UrlsTable.URLS.ORIGINAL_URL,
                UrlsTable.URLS.CREATED_AT,
                UrlsTable.URLS.EXPIRES_AT,
                UrlsTable.URLS.IS_ACTIVE,
                UrlsTable.URLS.TOTAL_CLICKS
            )
            .fetchOne() ?: throw RuntimeException("Failed to insert URL")

        return mapToModel(record)
    }

    override fun findByShortCode(shortCode: String): Urls? {
        val record = dsl.select(
                UrlsTable.URLS.ID,
                UrlsTable.URLS.SHORT_CODE,
                UrlsTable.URLS.ORIGINAL_URL,
                UrlsTable.URLS.CREATED_AT,
                UrlsTable.URLS.EXPIRES_AT,
                UrlsTable.URLS.IS_ACTIVE,
                UrlsTable.URLS.TOTAL_CLICKS
            )
            .from(UrlsTable.URLS)
            .where(UrlsTable.URLS.SHORT_CODE.eq(shortCode))
            .and(UrlsTable.URLS.IS_ACTIVE.eq(true))
            .fetchOneInto(UrlsRecord::class.java) ?: return null

        return mapToModel(record)
    }

    override fun existsByShortCode(shortCode: String): Boolean {
        return dsl.fetchExists(
            dsl.selectOne()
                .from(UrlsTable.URLS)
                .where(UrlsTable.URLS.SHORT_CODE.eq(shortCode))
        )
    }

    override fun incrementClickCount(shortCode: String): Int {
        return dsl.update(UrlsTable.URLS)
            .set(UrlsTable.URLS.TOTAL_CLICKS, UrlsTable.URLS.TOTAL_CLICKS.plus(1))
            .where(UrlsTable.URLS.SHORT_CODE.eq(shortCode))
            .execute()
    }

    override fun findByOriginalUrl(originalUrl: String): Urls? {
        return dsl.selectFrom(UrlsTable.URLS)
            .where(UrlsTable.URLS.ORIGINAL_URL.eq(originalUrl))
            .and(UrlsTable.URLS.IS_ACTIVE.eq(true))
            .fetchOne()
            ?.let { mapToModel(it) }
    }
    
    private fun mapToModel(record: UrlsRecord): Urls {
        return Urls(
            id = record.id!!,
            shortCode = record.shortCode!!,
            originalUrl = record.originalUrl!!,
            createdAt = record.createdAt!!,
            expiresAt = record.expiresAt,
            isActive = record.isActive!!,
            totalClicks = record.totalClicks!!
        )
    }
}
