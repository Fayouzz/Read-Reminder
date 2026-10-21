package com.personal.readreminder.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.readreminder.AppContainer
import com.personal.readreminder.data.*
import com.personal.readreminder.reminders.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReminderViewModel(private val c: AppContainer) : ViewModel() {
    private val dao = c.db.reminderDao()
    val reminders: StateFlow<List<ReminderEntity>> = dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val cfg: StateFlow<AppSettings> = c.settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val runtime: StateFlow<ReminderRuntime> = c.settings.runtime.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderRuntime())
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    fun canExact(): Boolean = c.scheduler.canScheduleExact()

    // Every change below is picked up by the app-wide watcher, which re-plans the single alarm.
    fun add(minutesOfDay: Int) = viewModelScope.launch { dao.insert(ReminderEntity(hour = minutesOfDay / 60, minute = minutesOfDay % 60)) }
    fun update(r: ReminderEntity) = viewModelScope.launch { dao.update(r) }
    fun delete(r: ReminderEntity) = viewModelScope.launch { dao.delete(r) }
    fun setMaster(on: Boolean) = viewModelScope.launch { c.settings.set(K.remEnabled, on) }

    fun sendTest() = viewModelScope.launch {
        val s = c.settings.settings.first()
        val shown = NotificationHelper.show(c.app, s, "This is a test reminder. Your real reminders look like this.", s.defaultTargetPages)
        messages.tryEmit(if (shown) "Test notification sent" else "Notifications are blocked. Allow them for Read Reminder in Android settings.")
    }
}
