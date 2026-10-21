package com.personal.readreminder.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.readreminder.data.*
import com.personal.readreminder.domain.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val loaded: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val pagesToday: Int = 0,
    val minutesToday: Int = 0,
    val goal: Int = 10,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val sessions: List<ReadingSessionEntity> = emptyList(),
    val books: List<BookProgress> = emptyList(),
    val currentBook: BookProgress? = null,
    val cfg: AppSettings = AppSettings(),
    val week: List<DayStatus> = emptyList(),
    val nextReminderAt: Long = 0L,
) {
    val progress: Float get() = if (goal <= 0) 0f else (pagesToday.toFloat() / goal).coerceIn(0f, 1f)
    val percent: Int get() = (progress * 100).toInt()
    val goalReached: Boolean get() = goal > 0 && pagesToday >= goal
    val remaining: Int get() = maxOf(0, goal - pagesToday)
    val weekGoalDays: Int get() = week.count { it.state == DayState.ACHIEVED }
    val message: String get() = when {
        goalReached && currentStreak > 1 -> "Another day of reading. Keep your streak alive!"
        goalReached -> "Amazing! You reached today's reading goal."
        pagesToday == 0 -> "Let's read a few pages today!"
        progress >= 0.75f -> "You're almost there!"
        else -> "Great start! Keep going."
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repo: ReadingRepository, private val settings: SettingsRepository) : ViewModel() {

    private val ticker = flow { while (true) { emit(Instant.now()); delay(30_000) } }

    private val today: Flow<LocalDate> = settings.settings.map { it.resetMinutes }.distinctUntilChanged()
        .flatMapLatest { reset -> ticker.map { DayBoundary.logicalDate(it, ZoneId.systemDefault(), reset) } }
        .distinctUntilChanged()

    val state: StateFlow<HomeUiState> = today.flatMapLatest { d ->
        val from = d.minusDays(400)
        combine(settings.settings, repo.observeSessions(d), repo.observeTotals(from, d), repo.observeGoals(from, d), repo.observeBookProgress()) { s, sessions, totals, goals, books ->
            val pagesByDay = totals.associate { LocalDate.ofEpochDay(it.day) to it.pages }
            val snap = goals.associate { LocalDate.ofEpochDay(it.date) to it.targetPages }
            val goalFor = { day: LocalDate -> snap[day] ?: s.dailyGoal }   // fall back to current goal if no snapshot
            val streaks = StreakCalculator.compute(d, pagesByDay, goalFor)
            val reading = books.filter { it.book.status == BookStatus.READING }
            HomeUiState(
                loaded = true, today = d, pagesToday = pagesByDay[d] ?: 0, minutesToday = sessions.sumOf { it.durationMinutes ?: 0 },
                goal = goalFor(d), currentStreak = streaks.current, longestStreak = streaks.longest, sessions = sessions.sortedBy { it.startTimestamp },
                books = books, currentBook = reading.firstOrNull { it.book.id == s.defaultBookId } ?: reading.firstOrNull(), cfg = s,
                week = StatsCalculator.weekStrip(d, s.weekStartMonday, pagesByDay, goalFor),
            )
        }
    }.combine(settings.runtime) { st, rt -> st.copy(nextReminderAt = rt.nextAt) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun log(input: SessionInput) = viewModelScope.launch { repo.addSession(input) }
    fun edit(old: ReadingSessionEntity, input: SessionInput) = viewModelScope.launch { repo.updateSession(old, input) }
    fun delete(s: ReadingSessionEntity) = viewModelScope.launch { repo.delete(s) }
    fun setGoal(pages: Int) = viewModelScope.launch { repo.setDailyGoalNow(pages) }
    fun setDefaultBook(id: Long) = viewModelScope.launch { settings.set(K.defBook, id) }
}
