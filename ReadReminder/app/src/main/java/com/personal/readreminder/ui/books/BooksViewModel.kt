package com.personal.readreminder.ui.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.readreminder.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BooksViewModel(private val repo: ReadingRepository, settings: SettingsRepository) : ViewModel() {
    val books: StateFlow<List<BookProgress>> = repo.observeBookProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val cfg: StateFlow<AppSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun save(b: BookEntity) = viewModelScope.launch { if (b.id == 0L) repo.addBook(b) else repo.updateBook(b) }
    fun delete(b: BookEntity) = viewModelScope.launch { repo.deleteBook(b) }

    fun setStatus(b: BookEntity, status: BookStatus) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        repo.updateBook(when (status) {
            BookStatus.COMPLETED -> b.copy(status = status, dateCompleted = now, dateStarted = b.dateStarted ?: now)
            BookStatus.READING -> b.copy(status = status, dateCompleted = null, dateStarted = b.dateStarted ?: now)
            BookStatus.ARCHIVED -> b.copy(status = status)
        })
    }

    fun log(i: SessionInput) = viewModelScope.launch { repo.addSession(i) }
}
