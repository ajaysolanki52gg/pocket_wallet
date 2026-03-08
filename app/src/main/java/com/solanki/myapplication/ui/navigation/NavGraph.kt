package com.solanki.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.solanki.myapplication.ui.screen.AnalyticsScreen
import com.solanki.myapplication.ui.screen.AddEditAccountScreen
import com.solanki.myapplication.ui.screen.AddEditTransactionScreen
import com.solanki.myapplication.ui.screen.HomeScreen
import com.solanki.myapplication.ui.screen.AccountListScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToAccountDetail = { accountId ->
                    navController.navigate(Screen.Analytics.createRoute(accountId, 0))
                },
                onNavigateToAddAccount = {
                    navController.navigate(Screen.AddEditAccount.createRoute())
                },
                onNavigateToAccountList = {
                    navController.navigate(Screen.AccountList.route)
                },
                onNavigateToAddTransaction = { accountId, transactionId ->
                    navController.navigate(Screen.AddEditTransaction.createRoute(accountId, transactionId))
                },
                onNavigateToCategorySpending = { accountId ->
                    navController.navigate(Screen.Analytics.createRoute(accountId, 1))
                },
                onNavigateToAnalytics = { accountId, initialPage ->
                    navController.navigate(Screen.Analytics.createRoute(accountId, initialPage))
                }
            )
        }
        composable(Screen.AccountList.route) {
            AccountListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditAccount = { accountId ->
                    navController.navigate(Screen.AddEditAccount.createRoute(accountId))
                }
            )
        }
        composable(
            route = Screen.Analytics.route,
            arguments = listOf(
                navArgument("accountId") { type = NavType.LongType },
                navArgument("initialPage") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: -1L
            val initialPage = backStackEntry.arguments?.getInt("initialPage") ?: 0
            AnalyticsScreen(
                accountId = accountId,
                initialPage = initialPage,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditTransaction = { accId, transId ->
                    navController.navigate(Screen.AddEditTransaction.createRoute(accId, transId))
                }
            )
        }
        composable(
            route = Screen.AddEditAccount.route,
            arguments = listOf(
                navArgument("accountId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: -1L
            AddEditAccountScreen(
                accountId = accountId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.AddEditTransaction.route,
            arguments = listOf(
                navArgument("accountId") { type = NavType.LongType },
                navArgument("transactionId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: -1L
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: -1L
            AddEditTransactionScreen(
                accountId = accountId,
                transactionId = transactionId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
