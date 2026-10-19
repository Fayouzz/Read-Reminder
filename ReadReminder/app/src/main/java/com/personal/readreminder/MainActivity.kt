package com.personal.readreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.personal.readreminder.ui.home.HomeScreen
import com.personal.readreminder.ui.home.HomeViewModel
import com.personal.readreminder.ui.settings.SettingsScreen
import com.personal.readreminder.ui.settings.SettingsViewModel
import com.personal.readreminder.ui.theme.ReadReminderTheme

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Default.Home),
    History("history", "History", Icons.Default.DateRange),
    Settings("settings", "Settings", Icons.Default.Settings),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val c = (application as ReadReminderApp).container
        setContent { ReadReminderTheme { Root(c) } }
    }
}

@Composable
private fun Root(c: AppContainer) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(selected = route == t.route, label = { Text(t.label) },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        })
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = Tab.Home.route, modifier = Modifier.padding(pad)) {
            composable(Tab.Home.route) {
                val vm: HomeViewModel = viewModel(factory = viewModelFactory { initializer { HomeViewModel(c.repository, c.settings) } })
                HomeScreen(vm)
            }
            composable(Tab.History.route) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("History, charts and statistics arrive in Stage 4.", Modifier.padding(24.dp)) }
            }
            composable(Tab.Settings.route) {
                val vm: SettingsViewModel = viewModel(factory = viewModelFactory { initializer { SettingsViewModel(c.repository, c.settings) } })
                SettingsScreen(vm)
            }
        }
    }
}
