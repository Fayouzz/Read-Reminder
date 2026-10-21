package com.personal.readreminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.personal.readreminder.data.AppSettings
import com.personal.readreminder.domain.AchievementType
import com.personal.readreminder.ui.achievements.AchievementsScreen
import com.personal.readreminder.ui.achievements.AchievementsViewModel
import com.personal.readreminder.ui.books.BooksScreen
import com.personal.readreminder.ui.books.BooksViewModel
import com.personal.readreminder.ui.common.Celebration
import com.personal.readreminder.ui.goal.GoalScreen
import com.personal.readreminder.ui.history.HistoryScreen
import com.personal.readreminder.ui.history.HistoryViewModel
import com.personal.readreminder.ui.home.HomeScreen
import com.personal.readreminder.ui.home.HomeViewModel
import com.personal.readreminder.ui.reminders.ReminderScreen
import com.personal.readreminder.ui.reminders.ReminderViewModel
import com.personal.readreminder.ui.settings.SettingsScreen
import com.personal.readreminder.ui.settings.SettingsViewModel
import com.personal.readreminder.ui.theme.ReadReminderTheme
import kotlinx.coroutines.launch

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Default.Home),
    History("history", "History", Icons.Default.DateRange),
    Settings("settings", "Settings", Icons.Default.Settings),
}

private fun tabFor(route: String?): Tab? = when (route) {
    "home", "books", "goal", "achievements" -> Tab.Home
    "history" -> Tab.History
    "settings", "reminders" -> Tab.Settings
    else -> null
}

class MainActivity : ComponentActivity() {
    private val container get() = (application as ReadReminderApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val c = container
        setContent {
            val cfg by c.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            ReadReminderTheme(cfg.theme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { App(c) }
            }
        }
    }

    /** Re-plan on return: picks up exact-alarm / notification permission changes made in system settings. */
    override fun onResume() {
        super.onResume()
        val c = container
        c.appScope.launch { c.scheduler.reschedule() }
    }
}

@Composable
private fun App(c: AppContainer) {
    val ctx = LocalContext.current
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val homeVm: HomeViewModel = viewModel(factory = viewModelFactory { initializer { HomeViewModel(c.repository, c.settings) } })

    // Android 13+ asks for notification permission; on Android 12 this is a no-op.
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    var queue by remember { mutableStateOf(listOf<AchievementType>()) }
    LaunchedEffect(Unit) { c.achievementEvents.collect { queue = queue + it } }

    fun go(r: String) = nav.navigate(r) { launchSingleTop = true }

    Scaffold(bottomBar = {
        NavigationBar {
            Tab.entries.forEach { t ->
                NavigationBarItem(selected = tabFor(route) == t, label = { Text(t.label) }, icon = { Icon(t.icon, contentDescription = t.label) },
                    onClick = {
                        nav.navigate(t.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    })
            }
        }
    }) { pad ->
        NavHost(nav, startDestination = Tab.Home.route, modifier = Modifier.padding(pad)) {
            composable("home") {
                HomeScreen(homeVm, onOpenBooks = { go("books") }, onOpenGoal = { go("goal") }, onOpenAchievements = { go("achievements") }, onOpenReminders = { go("reminders") })
            }
            composable("books") {
                val vm: BooksViewModel = viewModel(factory = viewModelFactory { initializer { BooksViewModel(c.repository, c.settings) } })
                BooksScreen(vm, onBack = { nav.popBackStack() })
            }
            composable("goal") { GoalScreen(homeVm, onBack = { nav.popBackStack() }) }
            composable("achievements") {
                val vm: AchievementsViewModel = viewModel(factory = viewModelFactory { initializer { AchievementsViewModel(c.repository) } })
                AchievementsScreen(vm, onBack = { nav.popBackStack() })
            }
            composable("history") {
                val vm: HistoryViewModel = viewModel(factory = viewModelFactory { initializer { HistoryViewModel(c.repository, c.settings) } })
                HistoryScreen(vm)
            }
            composable("settings") {
                val vm: SettingsViewModel = viewModel(factory = viewModelFactory { initializer { SettingsViewModel(c) } })
                SettingsScreen(vm, onOpenReminders = { go("reminders") }, onOpenGoal = { go("goal") })
            }
            composable("reminders") {
                val vm: ReminderViewModel = viewModel(factory = viewModelFactory { initializer { ReminderViewModel(c) } })
                ReminderScreen(vm, onBack = { nav.popBackStack() })
            }
        }
    }

    queue.firstOrNull()?.let { a ->
        key(a) { Celebration(a.emoji, "Achievement unlocked!", a.title + " \u2014 " + a.description) { queue = queue.drop(1) } }
    }
}
