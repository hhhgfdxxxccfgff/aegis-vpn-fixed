package com.example.vpn.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.AegisCyanPrimary
import com.example.vpn.engine.VpnController
import com.example.vpn.ui.dashboard.DashboardScreen
import com.example.vpn.ui.details.ConnectionDetailsScreen
import com.example.vpn.ui.logs.LogsScreen
import com.example.vpn.ui.profiles.ProfilesScreen
import com.example.vpn.ui.settings.SettingsScreen

sealed class Screen(
    val route: String,
    val title: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Security, Icons.Outlined.Security)
    data object Profiles : Screen("profiles", "Profiles", Icons.Filled.Folder, Icons.Outlined.Folder)
    data object Details : Screen("details", "Details", Icons.Filled.Info, Icons.Outlined.Info)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    data object Logs : Screen("logs", "Logs", Icons.Filled.Article, Icons.Outlined.Article)
}

@Composable
fun AegisApp(
    controller: VpnController,
    onThemeChanged: (String) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val screens = listOf(
        Screen.Dashboard,
        Screen.Profiles,
        Screen.Details,
        Screen.Settings,
        Screen.Logs
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                screens.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.activeIcon else screen.inactiveIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(text = screen.title, fontSize = 11.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.surface,
                            selectedTextColor = AegisCyanPrimary,
                            indicatorColor = AegisCyanPrimary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    controller = controller,
                    onNavigateToProfiles = { navController.navigate(Screen.Profiles.route) },
                    onNavigateToDetails = { navController.navigate(Screen.Details.route) },
                    onNavigateToLogs = { navController.navigate(Screen.Logs.route) }
                )
            }
            composable(Screen.Profiles.route) {
                ProfilesScreen(
                    controller = controller,
                    onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) }
                )
            }
            composable(Screen.Details.route) {
                ConnectionDetailsScreen(controller = controller)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(controller = controller, onThemeChanged = onThemeChanged)
            }
            composable(Screen.Logs.route) {
                LogsScreen(controller = controller)
            }
        }
    }
}
