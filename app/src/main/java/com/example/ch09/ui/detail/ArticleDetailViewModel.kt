package com.example.ch09.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ch09.data.NewsRepository
import com.example.ch09.data.model.Article
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArticleDetailUiState(
    val isLoading: Boolean = true,
    val article: Article? = null,
    val errorMessage: String? = null
)

// Tugas 2: layar detail. Yang dikirim lewat navigasi hanya URL artikel;
// datanya diambil dari repository, bukan objek Article utuh.
class ArticleDetailViewModel(
    private val repository: NewsRepository,
    private val articleUrl: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticleDetailUiState())
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val article = repository.getArticle(articleUrl)
            _uiState.update {
                if (article != null) {
                    it.copy(isLoading = false, article = article)
                } else {
                    it.copy(isLoading = false, errorMessage = "Artikel tidak ditemukan")
                }
            }
        }
    }
}
