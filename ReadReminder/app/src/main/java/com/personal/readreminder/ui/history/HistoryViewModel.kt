package com.personal.readreminder.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.readreminder.data.*
import com.personal.readreminder.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class HistoryMode { MONTH, YEAR }

data class HistoryUi(
    val mode: HistoryMode,
    val ym: YearMonth,
    val year: Int,
    val selectedDay: LocalDate,
    val today: LocalDate,
    val bars: List<Int>,
    val selectedBar: Int,
    val goalLine: Int?,
    val goalDaysPerMonth: List<Int>,
    val selectedPages: Int,
    val selectedGoal: Int,
    val week: List<DayStatus>,
    val stats: PeriodStats,
    val streaks: Streaks,
    val booksCompleted: Int,
    val records: List<ReadingSessionEntity>,
    val books: List<BookProgress>,
    val cfg: AppSettings,
    val hasAnyData: Boolean,
) {
    val canGoNext: Boolean get() = if (mode == HistoryMode.MONTH) ym < YearMonth.from(today) else year < today.year
}

class HistoryViewModel(private val repo: ReadingRepository, settings: SettingsRepository) : ViewModel() {
    private data class Sel(val mode: HistoryMode, val ym: YearMonth, val year: Int, val day: LocalDate)

    private val sel = MutableStateFlow(Sel(HistoryMode.MONTH, YearMonth.now(), YearMonth.now().year, LocalDate.now()))

    init {
        viewModelScope.launch {
            if (settings.settings.first().historyDefaultYear) sel.update { it.copy(mode = HistoryMode.YEAR) }
        }
    }

    val state: StateFlow<HistoryUi?> = combine(sel, repo.observeAllSessions(), repo.observeAllGoals(), repo.observeBookProgress(), settings.settings) { sel, sessions, goals, books, cfg ->
        build(sel, sessions, goals, books, cfg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun build(sel: Sel, sessions: List<ReadingSessionEntity>, goals: List<DailyGoalEntity>, books: List<BookProgress>, cfg: AppSettings): HistoryUi {
        val today = DayBoundary.logicalDate(Instant.now(), ZoneId.systemDefault(), cfg.resetMinutes)
        val byDay = StatsCalculator.pagesByDay(sessions)
        val snap = goals.associate { LocalDate.ofEpochDay(it.date) to it.targetPages }
        val goalFor = { d: LocalDate -> snap[d] ?: cfg.dailyGoal }
        val month = sel.mode == HistoryMode.MONTH
        val ym = sel.ym
        val from = if (month) ym.atDay(1) else LocalDate.of(sel.year, 1, 1)
        val to = if (month) ym.atEndOfMonth() else LocalDate.of(sel.year, 12, 31)
        val bars = if (month) (1..ym.lengthOfMonth()).map { byDay[ym.atDay(it)] ?: 0 }
        else (1..12).map { m -> YearMonth.of(sel.year, m).let { y -> (1..y.lengthOfMonth()).sumOf { byDay[y.atDay(it)] ?: 0 } } }
        val goalDaysPerMonth = (1..12).map { m ->
            YearMonth.of(sel.year, m).let { y -> (1..y.lengthOfMonth()).count { val d = y.atDay(it); !d.isAfter(today) && goalFor(d) > 0 && (byDay[d] ?: 0) >= goalFor(d) } }
        }
        val selectedBar = if (month) (if (YearMonth.from(sel.day) == ym) sel.day.dayOfMonth - 1 else -1) else (if (ym.year == sel.year) ym.monthValue - 1 else -1)
        val records = sessions.filter {
            val d = LocalDate.ofEpochDay(it.sessionDate)
            if (month) d == sel.day else YearMonth.from(d) == ym
        }.sortedBy { it.startTimestamp }
        return HistoryUi(
            mode = sel.mode, ym = ym, year = sel.year, selectedDay = sel.day, today = today, bars = bars, selectedBar = selectedBar,
            goalLine = if (month) goalFor(if (YearMonth.from(sel.day) == ym) sel.day else today) else null,
            goalDaysPerMonth = goalDaysPerMonth,
            selectedPages = byDay[sel.day] ?: 0, selectedGoal = goalFor(sel.day),
            week = StatsCalculator.weekStrip(sel.day, cfg.weekStartMonday, byDay, goalFor),
            stats = StatsCalculator.periodStats(sessions, from, to, today, goalFor),
            streaks = StreakCalculator.compute(today, byDay, goalFor),
            booksCompleted = books.count { it.book.status == BookStatus.COMPLETED },
            records = records, books = books, cfg = cfg, hasAnyData = sessions.isNotEmpty(),
        )
    }

    private fun defaultDay(ym: YearMonth): LocalDate { val t = LocalDate.now(); return if (YearMonth.from(t) == ym) t else ym.atDay(1) }

    fun setMode(m: HistoryMode) = sel.update { it.copy(mode = m) }

    fun move(delta: Int) = sel.update { s ->
        if (s.mode == HistoryMode.MONTH) {
            val ym = s.ym.plusMonths(delta.toLong()); s.copy(ym = ym, year = ym.year, day = defaultDay(ym))
        } else {
            val y = s.year + delta; val ym = YearMonth.of(y, s.ym.monthValue); s.copy(year = y, ym = ym, day = defaultDay(ym))
        }
    }

    fun selectBar(i: Int) = sel.update { s ->
        if (s.mode == HistoryMode.MONTH) s.copy(day = s.ym.atDay((i + 1).coerceIn(1, s.ym.lengthOfMonth())))
        else { val ym = YearMonth.of(s.year, (i + 1).coerceIn(1, 12)); s.copy(ym = ym, day = defaultDay(ym)) }
    }

    fun add(i: SessionInput) = viewModelScope.launch { repo.addSession(i) }
    fun edit(old: ReadingSessionEntity, i: SessionInput) = viewModelScope.launch { repo.updateSession(old, i) }
    fun delete(s: ReadingSessionEntity) = viewModelScope.launch { repo.delete(s) }
}
