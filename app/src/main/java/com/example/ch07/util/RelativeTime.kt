package com.example.ch07.util

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS
private const val DAY_MS = 24 * HOUR_MS

fun formatRelativeTime(now: Long, then: Long): String {
    val diff = maxOf(0L, now - then)

    return when {
        diff < MINUTE_MS -> "baru saja"
        diff < HOUR_MS -> "${diff / MINUTE_MS} menit lalu"
        diff < DAY_MS -> "${diff / HOUR_MS} jam lalu"
        else -> {
            val days = diff / DAY_MS
            when {
                days < 30 -> "$days hari lalu"
                days < 365 -> "${days / 30} bulan lalu"
                else -> "${days / 365} tahun lalu"
            }
        }
    }
}