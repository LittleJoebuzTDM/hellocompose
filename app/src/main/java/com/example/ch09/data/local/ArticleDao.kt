package com.example.ch09.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class ArticleDao {

    @Query("SELECT * FROM articles WHERE category = :category ORDER BY position")
    abstract suspend fun getArticles(category: String): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE url = :url LIMIT 1")
    abstract suspend fun getArticleByUrl(url: String): ArticleEntity?

    // Cari di seluruh cache. GROUP BY url karena artikel yang sama
    // bisa tersimpan di beberapa kategori. Pemanggil mengirim "%kata%".
    @Query(
        """
        SELECT * FROM articles
        WHERE title LIKE :query OR description LIKE :query
        GROUP BY url
        ORDER BY publishedAt DESC
        """
    )
    abstract suspend fun searchCached(query: String): List<ArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles WHERE category = :category")
    abstract suspend fun deleteCategory(category: String)

    // Hapus lalu isi ulang dalam satu transaksi: jika gagal di tengah,
    // cache lama tidak hilang. Karena itu DAO-nya abstract class.
    @Transaction
    open suspend fun replaceCategory(category: String, articles: List<ArticleEntity>) {
        deleteCategory(category)
        insertAll(articles)
    }
}