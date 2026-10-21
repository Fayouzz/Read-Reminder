package com.personal.readreminder

import android.app.Application

class ReadReminderApp : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }
}
