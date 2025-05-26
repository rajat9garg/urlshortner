package com.url.shortner.util

object Base62 {
    private const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private const val BASE = 62

    fun encode(number: Long): String {
        var n = number
        val sb = StringBuilder()
        if (n == 0L) return "0"
        while (n > 0) {
            sb.append(ALPHABET[(n % BASE).toInt()])
            n /= BASE
        }
        return sb.reverse().toString()
    }
}
