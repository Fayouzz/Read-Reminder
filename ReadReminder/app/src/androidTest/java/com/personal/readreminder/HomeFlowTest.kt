package com.personal.readreminder

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

/** Instrumented UI flows. Run on a device/emulator: ./gradlew connectedDebugAndroidTest (not run by the GitHub build). */
class HomeFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun logPagesShowsUpOnHome() {
        rule.onNodeWithText("Log Reading").performClick()
        rule.onNodeWithText("Pages read in this session").performTextInput("10")
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("10 pages").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun navigationTabsOpen() {
        rule.onNodeWithText("History").performClick()
        rule.onNodeWithText("Month").assertExists()
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithText("Advanced Settings").assertExists()
    }

    @Test fun remindersScreenShowsSchedule() {
        rule.onNodeWithText("Settings").performClick()
        rule.onNodeWithText("Reminder schedule").performClick()
        rule.onNodeWithText("Reminder Schedule").assertExists()
    }
}
