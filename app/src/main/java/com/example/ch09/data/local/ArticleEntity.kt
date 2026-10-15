package com.example.ch09.data.local

import androidx.room.Entity

// Satu artikel bisa muncul di beberapa kategori, jadi kuncinya gabungan.
@Entity(
    tableName = "articles",
    primaryKeys = ["category", "url"]
)
data class ArticleEntity(
    val category: String,      // nama kategori, atau "__search__" untuk hasil pencarian
    val url: String,
    val title: String,
    val author: String,
    val description: String,
    val content: String,
    val imageUrl: String?,
    val publishedAt: String,
    val sourceName: String,
    val position: Int,         // urutan di daftar, supaya urutan dari API terjaga
    val cachedAt: Long         // waktu disimpan (epoch millis)
)