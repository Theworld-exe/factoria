package com.factoria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.factoria.core.designsystem.*
import com.factoria.navigation.Screen
import com.factoria.ui.blueprint.BlueprintScreen
import com.factoria.ui.calculator.CalculatorScreen
import com.factoria.ui.codex.CodexScreen
import com.factoria.ui.checklist.ChecklistScreen
import com.factoria.ui.dashboard.DashboardScreen
import com.factoria.ui.datapack.DataPackScreen
import com.factoria.ui.enemies.EnemiesScreen
import com.factoria.ui.logistics.LogisticsScreen
import com.factoria.ui.more.MoreScreen
import com.factoria.ui.paywall.PaywallDialog
import com.factoria.ui.paywall.UserPreferencesManager
import com.factoria.ui.planets.PlanetsScreen
import com.factoria.ui.recipes.RecipesScreen
import com.factoria.ui.techtree.TechTreeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FactoriaTheme {
                val context = LocalContext.current
                val userPrefsManager = remember { UserPreferencesManager.getInstance(context) }
                val isAdFreeUnlocked by userPrefsManager.isAdFreeUnlocked.collectAsState()
                var showPaywall by remember { mutableStateOf(false) }

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

                val currentScreenTitle = when (currentRoute) {
                    Screen.Dashboard.route -> "Dashboard"
                    Screen.Calculator.route -> "Production Calculator"
                    Screen.Codex.route -> "Codex & Database"
                    Screen.Blueprints.route -> "Blueprint Inspector"
                    Screen.Logistics.route -> "Logistics & Power"
                    Screen.More.route -> "More Tools & Guides"
                    Screen.TechTree.route -> "Technology Tree"
                    Screen.Enemies.route -> "Enemies Bestiary"
                    Screen.Planets.route -> "Space Age Planets"
                    Screen.Recipes.route -> "All In-Game Recipes"
                    Screen.Checklist.route -> "Factory Todo Checklist"
                    Screen.DataPack.route -> "Data Pack Info"
                    else -> "Factoria"
                }

                val canNavigateBack = currentRoute != Screen.Dashboard.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        FactoriaTopAppBar(
                            title = currentScreenTitle,
                            subtitle = "Factorio 2.0 + Space Age",
                            navigationIcon = if (canNavigateBack) {
                                {
                                    IconButton(onClick = {
                                        FactoriaSoundManager.playClick()
                                        if (!navController.popBackStack()) {
                                            navController.navigate(Screen.Dashboard.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                            }
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = TextPrimary
                                        )
                                    }
                                }
                            } else null
                        )
                    },
                    bottomBar = {
                        Column {
                            if (!isAdFreeUnlocked) {
                                AdBannerStrip(
                                    onRemoveAdsClick = { showPaywall = true }
                                )
                            }
                            NavigationBar(
                                containerColor = SlateSurface,
                                contentColor = TextPrimary
                            ) {
                                Screen.bottomNavItems.forEach { screen ->
                                    val selected = currentRoute == screen.route
                                    NavigationBarItem(
                                        selected = selected,
                                        alwaysShowLabel = true,
                                        onClick = {
                                            FactoriaSoundManager.playTabSwitch()
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
                                                imageVector = screen.icon,
                                                contentDescription = screen.title
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = screen.title,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.Black,
                                            selectedTextColor = FactorioOrange,
                                            indicatorColor = FactorioOrange,
                                            unselectedIconColor = TextSecondary,
                                            unselectedTextColor = TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    },
                    containerColor = SlateBackground
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(onNavigate = { route ->
                                navController.navigate(route)
                            })
                        }
                        composable(Screen.Calculator.route) {
                            CalculatorScreen()
                        }
                        composable(Screen.Codex.route) {
                            CodexScreen()
                        }
                        composable(Screen.Blueprints.route) {
                            BlueprintScreen()
                        }
                        composable(Screen.Logistics.route) {
                            LogisticsScreen()
                        }
                        composable(Screen.More.route) {
                            MoreScreen(onNavigate = { route ->
                                navController.navigate(route)
                            })
                        }
                        composable(Screen.TechTree.route) {
                            TechTreeScreen()
                        }
                        composable(Screen.Enemies.route) {
                            EnemiesScreen()
                        }
                        composable(Screen.Planets.route) {
                            PlanetsScreen()
                        }
                        composable(Screen.Recipes.route) {
                            RecipesScreen()
                        }
                        composable(Screen.Checklist.route) {
                            ChecklistScreen()
                        }
                        composable(Screen.DataPack.route) {
                            DataPackScreen()
                        }
                    }
                }

                if (showPaywall) {
                    PaywallDialog(
                        onDismiss = { showPaywall = false },
                        onUnlockSuccess = { userPrefsManager.unlockAdFree() }
                    )
                }
            }
        }
    }
}
