package com.personal.readreminder.ui.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.readreminder.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** currentPage is derived: starting page + pages from logged sessions, so it always matches history. */
data class BookUi(val book: BookEntity, val currentPage: Int) {
    val fraction: Float? = book.totalPages?.takeIf { it > 0 }?.let { (currentPage.toFloat() / it).coerceIn(0f, 1f) }
    val reachedEnd: Boolean = book.totalPages?.let { it > 0 && currentPage >= it } ?: false
}

class BooksViewModel(private val repo: ReadingRepository) : ViewModel() {
    val books: StateFlow<List<BookUi>> = combine(repo.observeBooks(), repo.observeBookPages()) { books, pages ->
        val m = pages.associate { it.bookId to it.pages }
        books.map { BookUi(it, it.initialPage + (m[it.id] ?: 0)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun log(i: SessionInput) = viewModelScope.launch { repo.logNow(i) }
}
