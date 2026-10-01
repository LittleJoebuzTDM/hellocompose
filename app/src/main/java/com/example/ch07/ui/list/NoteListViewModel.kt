package com.example.ch07.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ch07.data.NoteEntity
import com.example.ch07.data.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NoteListUiState(
    val isLoading: Boolean = false,
    val notes: List<NoteEntity> = emptyList(),
    val query: String = "",
    val recentlyDeleted: NoteEntity? = null,
    val availableTags: List<String> = emptyList(),
    val selectedTag: String? = null,
    val errorMessage: String? = null
) {
    // Turunan: sedang mencari/memfilter atau tidak (menentukan pesan kosong).
    val isFiltering: Boolean
        get() = query.isNotBlank() || selectedTag != null
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class NoteListViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    // Teks yang sedang diketik (langsung tampil di TextField)
    private val queryText = MutableStateFlow("")
    private val loadError = MutableStateFlow<String?>(null)
    private val recentlyDeleted = MutableStateFlow<NoteEntity?>(null)
    private val selectedTag = MutableStateFlow<String?>(null)

    // Query ke database ditunda 300 ms setelah pengguna berhenti mengetik
    // (debounce), dan hanya query TERBARU yang diamati (flatMapLatest).
    // Query kosong tidak ditunda: daftar penuh tampil segera.
    private val searchResults: Flow<List<NoteEntity>> = queryText
        .debounce { query -> if (query.isBlank()) 0L else 300L }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getAllNotes()
            else repository.searchNotes(query)
        }
        .catch { e ->
            loadError.value = e.message ?: "Gagal memuat catatan"
            emit(emptyList())
        }

    // UiState DITURUNKAN dari beberapa sumber (pola Chapter 6).
    private val allTags: Flow<List<String>> = repository.getAllNotes()
        .map { notes ->
            notes.flatMap { it.tags }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
        .catch { emit(emptyList()) }

    // Gabungkan dua flow dulu supaya combine utama tetap 5 flow
    private val tagState: Flow<Pair<List<String>, String?>> =
        combine(allTags, selectedTag) { tags, selected -> tags to selected }

    init {
        // Tag yang sudah tidak dipakai catatan mana pun otomatis dilepas
        viewModelScope.launch {
            allTags.collect { tags ->
                val current = selectedTag.value
                if (current != null && current !in tags) selectedTag.value = null
            }
        }
    }
    val uiState: StateFlow<NoteListUiState> = combine(
        searchResults, queryText, loadError, recentlyDeleted, tagState
    ) { results, query, error, deleted, (tags, selected) ->
        val activeTag = selected?.takeIf { it in tags }
        NoteListUiState(
            notes = if (activeTag == null) results else results.filter { activeTag in it.tags },
            query = query,
            recentlyDeleted = deleted,
            availableTags = tags,
            selectedTag = activeTag,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NoteListUiState(isLoading = true)
    )

    fun onQueryChange(newQuery: String) {
        queryText.value = newQuery
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            recentlyDeleted.value = note
        }
    }

    fun undoDelete() {
        val note = recentlyDeleted.value ?: return
        recentlyDeleted.value = null
        viewModelScope.launch { repository.insertNote(note) }
    }

    fun onUndoDismissed() {
        recentlyDeleted.value = null
    }

    fun onTagSelected(tag: String?) {
        // Ketuk tag yang sedang aktif = lepas filter; null = "Semua"
        selectedTag.value = if (tag == null || tag == selectedTag.value) null else tag
    }

    // Menyematkan tidak mengubah updatedAt (bukan perubahan isi catatan)
    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned))
        }
    }
}
