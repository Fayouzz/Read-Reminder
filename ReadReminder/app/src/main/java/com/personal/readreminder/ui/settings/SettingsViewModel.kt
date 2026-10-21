package com.personal.readreminder.ui.settings

import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.readreminder.AppContainer
import com.personal.readreminder.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val cfg: StateFlow<AppSettings> = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val books: StateFlow<List<BookProgress>> = c.repository.observeBookProgress().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    fun <T> set(key: Preferences.Key<T>, value: T) { viewModelScope.launch { c.settings.set(key, value) } }

    fun setReset(minutes: Int) {
        viewModelScope.launch { c.repository.setResetMinutes(minutes); messages.tryEmit("Daily reset time updated") }
    }

    private fun write(uri: Uri, text: String) {
        val out = c.app.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot open the file")
        out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { write(uri, c.backup.csv()) }
                .onSuccess { messages.tryEmit("Reading history exported") }
                .onFailure { messages.tryEmit("Export failed: ${it.message}") }
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { write(uri, c.backup.export()) }
                .onSuccess { messages.tryEmit("Backup saved") }
                .onFailure { messages.tryEmit("Backup failed: ${it.message}") }
        }
    }

    fun restore(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bytes = c.app.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw BackupException("Cannot open the file")
                if (bytes.size > 20_000_000) throw BackupException("The file is too large to be a backup")
                c.backup.restore(bytes.toString(Charsets.UTF_8))
                c.scheduler.reschedule()
                messages.tryEmit("Backup restored")
            } catch (e: BackupException) {
                messages.tryEmit("Restore failed, nothing was changed: ${e.message}")
            } catch (e: Exception) {
                messages.tryEmit("Restore failed: ${e.message}")
            }
        }
    }

    fun resetStats() { viewModelScope.launch { c.repository.resetStatistics(); messages.tryEmit("Statistics reset") } }

    fun deleteAll() {
        viewModelScope.launch {
            c.repository.deleteAll(); c.settings.clearAll(); c.seedDefaults(); c.scheduler.reschedule()
            messages.tryEmit("All local data deleted")
        }
    }
}
