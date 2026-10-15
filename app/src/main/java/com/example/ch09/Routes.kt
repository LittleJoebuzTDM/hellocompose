package com.example.ch09

import android.net.Uri

sealed class Routes(val route: String) {
    data object News : Routes("news")

    data object Detail : Routes("detail/{articleUrl}") {
        const val ARG_ARTICLE_URL = "articleUrl"

        fun createRoute(articleUrl: String): String =
            "detail/${Uri.encode(articleUrl)}"
    }
}
