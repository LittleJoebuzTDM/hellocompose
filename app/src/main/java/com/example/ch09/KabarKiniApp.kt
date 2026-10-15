package com.example.ch09

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ch09.ui.detail.ArticleDetailRoute
import com.example.ch09.ui.news.NewsRoute

@Composable
fun KabarKiniApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.News.route
    ) {
        composable(Routes.News.route) {
            NewsRoute(
                onArticleClick = { article ->
                    navController.navigate(Routes.Detail.createRoute(article.url))
                }
            )
        }

        composable(
            route = Routes.Detail.route,
            arguments = listOf(
                navArgument(Routes.Detail.ARG_ARTICLE_URL) {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            ArticleDetailRoute(
                articleUrl = entry.arguments?.getString(Routes.Detail.ARG_ARTICLE_URL).orEmpty(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}