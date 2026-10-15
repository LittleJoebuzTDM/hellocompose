package com.example.ch09.data

import com.example.ch09.data.local.ArticleDao
import com.example.ch09.data.local.ArticleEntity
import com.example.ch09.data.mapper.toDomain
import com.example.ch09.data.mapper.toEntity
import com.example.ch09.data.model.Article
import com.example.ch09.data.model.NewsCategory
import com.example.ch09.data.model.NewsFeed
import com.example.ch09.data.model.OfflineInfo // ADJUST: check the real package
import com.example.ch09.data.remote.NewsApiService
import com.example.ch09.data.remote.dto.NewsResponseDto
import kotlin.coroutines.cancellation.CancellationException

interface NewsRepository {
    suspend fun getTopHeadlines(category: NewsCategory): Result<NewsFeed>
    suspend fun searchNews(query: String): Result<NewsFeed>
    // Untuk layar detail (Tugas 2): artikel apa pun yang pernah dimuat
    suspend fun getArticle(url: String): Article?
}

private const val SEARCH_KEY = "__search__"

class NewsRepositoryImpl(
    private val api: NewsApiService,
    private val dao: ArticleDao,
    private val clock: () -> Long = System::currentTimeMillis
) : NewsRepository {

    override suspend fun getTopHeadlines(category: NewsCategory): Result<NewsFeed> =
        fetch(
            cacheKey = category.apiValue,
            remote = { api.getTopHeadlines(category = category.apiValue) },
            readCache = { dao.getArticles(category.apiValue) }
        )

    override suspend fun searchNews(query: String): Result<NewsFeed> =
        fetch(
            cacheKey = SEARCH_KEY,
            remote = { api.searchNews(query = query) },
            // Offline: cari di seluruh cache, bukan hanya hasil pencarian terakhir
            readCache = { dao.searchCached("%$query%") }
        )

    override suspend fun getArticle(url: String): Article? =
        dao.getArticleByUrl(url)?.toDomain()

    private suspend fun fetch(
        cacheKey: String,
        remote: suspend () -> NewsResponseDto,
        readCache: suspend () -> List<ArticleEntity>
    ): Result<NewsFeed> {
        val result = runCatching {
            remote().articles
                .mapNotNull { it.toDomain() }
                // NewsAPI bisa mengirim url ganda; LazyColumn(key = url) akan crash
                .distinctBy { it.url }
        }

        // runCatching juga menangkap CancellationException (mis. pengguna sudah
        // pindah kategori). Itu BUKAN error jaringan: lempar ulang.
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }

        // Berhasil: simpan ke Room, lalu kembalikan data segar
        result.getOrNull()?.let { articles ->
            val now = clock()
            dao.replaceCategory(
                cacheKey,
                articles.mapIndexed { index, article -> article.toEntity(cacheKey, index, now) }
            )
            return Result.success(NewsFeed(articles))
        }

        // Gagal: pakai cache jika ada
        val error = result.exceptionOrNull() ?: return result.map { NewsFeed(it) }
        val cached = readCache()
        if (cached.isEmpty()) return Result.failure(error)

        return Result.success(
            NewsFeed(
                articles = cached.map { it.toDomain() },
                offline = OfflineInfo(
                    cachedAt = cached.minOf { it.cachedAt },
                    reason = error.toUserMessage()
                )
            )
        )
    }
}