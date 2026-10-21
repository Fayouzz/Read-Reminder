package com.personal.readreminder.data

import androidx.room.withTransaction
import com.personal.readreminder.domain.AchievementType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class BackupException(message: String) : Exception(message)

data class BackupData(
    val exportedAt: Long,
    val books: List<BookEntity>,
    val sessions: List<ReadingSessionEntity>,
    val goals: List<DailyGoalEntity>,
    val reminders: List<ReminderEntity>,
    val achievements: List<AchievementEntity>,
    val settings: Map<String, Any>,
)

/** Pure JSON encode/decode with full validation; nothing touches the database until decode succeeded. */
object BackupCodec {
    const val FORMAT = "read-reminder-backup"
    const val VERSION = 1

    private fun fail(msg: String): Nothing = throw BackupException(msg)
    private fun check(cond: Boolean, msg: String) { if (!cond) fail(msg) }
    private fun JSONObject.strOrNull(k: String): String? = if (isNull(k)) null else getString(k)
    private fun JSONObject.longOrNull(k: String): Long? = if (isNull(k)) null else getLong(k)
    private fun JSONObject.intOrNull(k: String): Int? = if (isNull(k)) null else getInt(k)
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun nz(v: Any?): Any = v ?: JSONObject.NULL

    fun encode(d: BackupData): String {
        val root = JSONObject()
        root.put("format", FORMAT); root.put("version", VERSION); root.put("exportedAt", d.exportedAt)
        root.put("books", JSONArray().also { a -> d.books.forEach { b -> a.put(JSONObject().also { o ->
            o.put("id", b.id); o.put("title", b.title); o.put("author", nz(b.author)); o.put("totalPages", nz(b.totalPages))
            o.put("initialPage", b.initialPage); o.put("status", b.status.name); o.put("coverUri", nz(b.coverUri))
            o.put("coverColor", nz(b.coverColor)); o.put("dateAdded", b.dateAdded); o.put("dateStarted", nz(b.dateStarted))
            o.put("dateCompleted", nz(b.dateCompleted)); o.put("notes", nz(b.notes)) }) } })
        root.put("sessions", JSONArray().also { a -> d.sessions.forEach { s -> a.put(JSONObject().also { o ->
            o.put("id", s.id); o.put("bookId", nz(s.bookId)); o.put("sessionDate", s.sessionDate); o.put("startTimestamp", s.startTimestamp)
            o.put("endTimestamp", nz(s.endTimestamp)); o.put("pagesRead", s.pagesRead); o.put("startPage", nz(s.startPage))
            o.put("endPage", nz(s.endPage)); o.put("durationMinutes", nz(s.durationMinutes)); o.put("notes", nz(s.notes))
            o.put("createdAt", s.createdAt); o.put("updatedAt", s.updatedAt) }) } })
        root.put("goals", JSONArray().also { a -> d.goals.forEach { g -> a.put(JSONObject().also { o ->
            o.put("date", g.date); o.put("targetPages", g.targetPages); o.put("resetMinutes", g.resetMinutes) }) } })
        root.put("reminders", JSONArray().also { a -> d.reminders.forEach { r -> a.put(JSONObject().also { o ->
            o.put("id", r.id); o.put("hour", r.hour); o.put("minute", r.minute); o.put("enabled", r.enabled)
            o.put("weekdayMask", r.weekdayMask); o.put("message", nz(r.message)); o.put("mode", r.mode)
            o.put("intervalMinutes", nz(r.intervalMinutes)); o.put("targetPages", nz(r.targetPages)) }) } })
        root.put("achievements", JSONArray().also { a -> d.achievements.forEach { x -> a.put(JSONObject().also { o ->
            o.put("type", x.achievementType); o.put("unlockedAt", x.unlockedAt) }) } })
        val st = JSONObject()
        d.settings.forEach { (k, v) ->
            val tag = when (v) { is Int -> "i"; is Long -> "l"; is Boolean -> "b"; is String -> "s"; else -> null }
            if (tag != null) st.put(k, JSONObject().also { it.put("t", tag); it.put("v", v) })
        }
        root.put("settings", st)
        return root.toString(2)
    }

    fun decode(text: String): BackupData {
        try {
            val root = JSONObject(text)
            check(root.optString("format") == FORMAT, "This is not a Read Reminder backup file")
            val version = root.getInt("version")
            check(version in 1..VERSION, "Unsupported backup version $version")

            val bookIds = HashSet<Long>()
            val books = root.getJSONArray("books").objects().map { o ->
                val id = o.getLong("id"); check(id > 0 && bookIds.add(id), "Duplicate or invalid book id")
                val title = o.getString("title"); check(title.isNotBlank(), "A book has no title")
                val status = runCatching { BookStatus.valueOf(o.getString("status")) }.getOrNull() ?: fail("Unknown book status")
                val total = o.intOrNull("totalPages"); check(total == null || total > 0, "Invalid total pages for \"$title\"")
                val initial = o.getInt("initialPage"); check(initial >= 0, "Invalid starting page for \"$title\"")
                BookEntity(id, title, o.strOrNull("author"), total, initial, status, o.strOrNull("coverUri"), o.intOrNull("coverColor"),
                    o.getLong("dateAdded"), o.longOrNull("dateStarted"), o.longOrNull("dateCompleted"), o.strOrNull("notes"))
            }
            val sessionIds = HashSet<Long>()
            val sessions = root.getJSONArray("sessions").objects().map { o ->
                val id = o.getLong("id"); check(id > 0 && sessionIds.add(id), "Duplicate or invalid session id")
                val pages = o.getInt("pagesRead"); check(pages >= 0, "A session has negative pages")
                val dur = o.intOrNull("durationMinutes"); check(dur == null || dur >= 0, "A session has a negative duration")
                val bid = o.longOrNull("bookId")?.takeIf { it in bookIds }
                ReadingSessionEntity(id, bid, o.getLong("sessionDate"), o.getLong("startTimestamp"), o.longOrNull("endTimestamp"),
                    pages, o.intOrNull("startPage"), o.intOrNull("endPage"), dur, o.strOrNull("notes"),
                    o.getLong("createdAt"), o.getLong("updatedAt"))
            }
            val goals = root.getJSONArray("goals").objects().map { o ->
                val t = o.getInt("targetPages"); check(t > 0, "A daily goal is not positive")
                DailyGoalEntity(o.getLong("date"), t, o.optInt("resetMinutes", 0))
            }.distinctBy { it.date }
            val reminderIds = HashSet<Long>()
            val reminders = root.getJSONArray("reminders").objects().map { o ->
                val id = o.getLong("id"); check(id > 0 && reminderIds.add(id), "Duplicate or invalid reminder id")
                val h = o.getInt("hour"); val m = o.getInt("minute"); val mask = o.getInt("weekdayMask")
                check(h in 0..23 && m in 0..59, "A reminder has an invalid time"); check(mask in 1..127, "A reminder has invalid weekdays")
                val tp = o.intOrNull("targetPages"); check(tp == null || tp > 0, "A reminder has an invalid page target")
                ReminderEntity(id, h, m, o.getBoolean("enabled"), mask, o.strOrNull("message"), o.optString("mode", "FIXED"),
                    o.intOrNull("intervalMinutes"), tp)
            }
            val valid = AchievementType.entries.map { it.name }.toSet()
            val achievements = root.getJSONArray("achievements").objects().map { o ->
                val t = o.getString("type"); check(t in valid, "Unknown achievement \"$t\"")
                AchievementEntity(t, o.getLong("unlockedAt"))
            }.distinctBy { it.achievementType }
            val settings = LinkedHashMap<String, Any>()
            root.optJSONObject("settings")?.let { st ->
                st.keys().forEach { k ->
                    val o = st.getJSONObject(k)
                    when (o.getString("t")) {
                        "i" -> settings[k] = o.getInt("v"); "l" -> settings[k] = o.getLong("v")
                        "b" -> settings[k] = o.getBoolean("v"); "s" -> settings[k] = o.getString("v")
                        else -> fail("Unknown setting type")
                    }
                }
            }
            return BackupData(root.optLong("exportedAt", 0L), books, sessions, goals, reminders, achievements, settings)
        } catch (e: JSONException) {
            throw BackupException("The file is damaged or not a valid backup (${e.message})")
        }
    }
}

object CsvExporter {
    private fun esc(v: String?): String {
        if (v.isNullOrEmpty()) return ""
        // Leading = + - @ would be run as a formula by spreadsheets, so neutralise it.
        val safe = if (v[0] in "=+-@") "'$v" else v
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    fun sessionsToCsv(sessions: List<ReadingSessionEntity>, books: List<BookEntity>, zone: ZoneId): String {
        val byId = books.associateBy { it.id }
        val sb = StringBuilder("date,time,book,author,pages_read,start_page,end_page,duration_minutes,notes\n")
        for (s in sessions.sortedBy { it.startTimestamp }) {
            val b = s.bookId?.let { byId[it] }
            val time = Instant.ofEpochMilli(s.startTimestamp).atZone(zone).toLocalTime().withSecond(0).withNano(0)
            sb.append(LocalDate.ofEpochDay(s.sessionDate)).append(',').append(time).append(',')
                .append(esc(b?.title)).append(',').append(esc(b?.author)).append(',').append(s.pagesRead).append(',')
                .append(s.startPage ?: "").append(',').append(s.endPage ?: "").append(',').append(s.durationMinutes ?: "").append(',')
                .append(esc(s.notes)).append('\n')
        }
        return sb.toString()
    }
}

class BackupManager(private val db: AppDatabase, private val settings: SettingsRepository) {
    suspend fun export(): String = BackupCodec.encode(BackupData(
        System.currentTimeMillis(), db.bookDao().getAll(), db.sessionDao().getAll(), db.goalDao().getAll(),
        db.reminderDao().getAllOnce(), db.achievementDao().getAll(), settings.exportMap()))

    suspend fun csv(): String = CsvExporter.sessionsToCsv(db.sessionDao().getAll(), db.bookDao().getAll(), ZoneId.systemDefault())

    /** Validates the whole file first; on any problem the database is left untouched. */
    suspend fun restore(text: String) {
        val d = BackupCodec.decode(text)
        db.withTransaction {
            db.sessionDao().deleteAll(); db.bookDao().deleteAll(); db.goalDao().deleteAll()
            db.reminderDao().deleteAll(); db.achievementDao().deleteAll()
            db.bookDao().insertAll(d.books); db.sessionDao().insertAll(d.sessions); db.goalDao().insertAll(d.goals)
            db.reminderDao().insertAll(d.reminders); db.achievementDao().insertAll(d.achievements)
        }
        settings.restore(d.settings)
    }
}
