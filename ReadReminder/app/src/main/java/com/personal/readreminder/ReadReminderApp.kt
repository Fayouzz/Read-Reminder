package com.personal.readreminder

import android.app.Application
import com.personal.readreminder.data.AppDatabase
import com.personal.readreminder.data.ReadingRepository
import com.personal.readreminder.data.SettingsRepository

/** Simple manual DI container (no Hilt needed at this size). */
class AppContainer(app: Application) {
    val db = AppDatabase.build(app)
    val settings = SettingsRepository(app)
    val repository = ReadingRepository(db.sessionDao(), db.goalDao(), settings)
}

class ReadReminderApp : Application() {
    lateinit var container: AppContainer
    override fun onCreate() { super.onCreate(); container = AppContainer(this) }
}
