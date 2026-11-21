package com.factoria.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Calculator : Screen("calculator", "Calculator", Icons.Default.Calculate)
    data object Codex : Screen("codex", "Codex", Icons.AutoMirrored.Filled.MenuBook)
    data object Logistics : Screen("logistics", "Logistics", Icons.Default.ElectricBolt)
    data object More : Screen("more", "More", Icons.Default.MoreHoriz)

    // Secondary navigation destinations
    data object Blueprints : Screen("blueprints", "Blueprints", Icons.Default.Description)
    data object TechTree : Screen("techtree", "Tech Tree", Icons.Default.AccountTree)
    data object Enemies : Screen("enemies", "Enemies", Icons.Default.BugReport)
    data object Planets : Screen("planets", "Planets", Icons.Default.Public)
    data object Recipes : Screen("recipes", "Recipes", Icons.Default.PrecisionManufacturing)
    data object Checklist : Screen("checklist", "Checklist", Icons.Default.Checklist)
    data object DataPack : Screen("datapack", "Data Info", Icons.Default.Storage)

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(
                Dashboard,
                Calculator,
                Codex,
                Logistics,
                More
            )
    }
}
