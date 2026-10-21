package com.personal.readreminder

import com.personal.readreminder.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class BackupCodecTest {
    private val book = BookEntity(id = 1, title = "Dune", author = "Herbert", totalPages = 600, dateAdded = 1000)
    private val session = ReadingSessionEntity(id = 1, bookId = 1, sessionDate = 20000, startTimestamp = 5, pagesRead = 12, durationMinutes = 30, notes = "good, \"very\"", createdAt = 1, updatedAt = 2)
    private val data = BackupData(42, listOf(book), listOf(session), listOf(DailyGoalEntity(20000, 10)),
        listOf(ReminderEntity(id = 1, hour = 7, minute = 30)), listOf(AchievementEntity("FIRST_SESSION", 99)),
        mapOf("daily_goal" to 15, "default_book" to 1L, "vibrate" to false, "display_name" to "Sam"))

    @Test fun roundTrip() {
        val back = BackupCodec.decode(BackupCodec.encode(data))
        assertEquals(data.books, back.books); assertEquals(data.sessions, back.sessions); assertEquals(data.goals, back.goals)
        assertEquals(data.reminders, back.reminders); assertEquals(data.achievements, back.achievements)
        assertEquals(15, (back.settings["daily_goal"] as Number).toInt()); assertEquals("Sam", back.settings["display_name"])
        assertEquals(false, back.settings["vibrate"])
    }

    private fun rejects(text: String): Boolean = try { BackupCodec.decode(text); false } catch (e: BackupException) { true }

    @Test fun rejectsGarbageAndWrongFormat() {
        assertTrue(rejects("not json")); assertTrue(rejects("{}")); assertTrue(rejects("""{"format":"other","version":1}"""))
    }

    @Test fun rejectsNewerVersion() = assertTrue(rejects(BackupCodec.encode(data).replace("\"version\": 1", "\"version\": 99")))

    @Test fun rejectsNegativePages() = assertTrue(rejects(BackupCodec.encode(data).replace("\"pagesRead\": 12", "\"pagesRead\": -3")))

    @Test fun rejectsInvalidReminderTime() = assertTrue(rejects(BackupCodec.encode(data).replace("\"hour\": 7", "\"hour\": 27")))

    @Test fun unknownBookReferenceBecomesNull() {
        val orphan = BackupData(1, emptyList(), listOf(session), emptyList(), emptyList(), emptyList(), emptyMap())
        assertNull(BackupCodec.decode(BackupCodec.encode(orphan)).sessions.single().bookId)
    }

    @Test fun csvEscapesAndNeutralisesFormulas() {
        val b = book.copy(title = "=SUM(A1)")
        val csv = CsvExporter.sessionsToCsv(listOf(session), listOf(b), ZoneId.of("UTC"))
        assertTrue(csv.startsWith("date,time,book,author,pages_read"))
        assertTrue(csv.contains("'=SUM(A1)")); assertTrue(csv.contains("\"good, \"\"very\"\"\""))
    }
}
