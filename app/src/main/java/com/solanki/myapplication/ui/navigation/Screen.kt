package com.solanki.myapplication.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Analytics : Screen("analytics/{accountId}/{initialPage}") {
        fun createRoute(accountId: Long = -1L, initialPage: Int = 0) = "analytics/$accountId/$initialPage"
    }
    object AddEditAccount : Screen("add_edit_account?accountId={accountId}") {
        fun createRoute(accountId: Long = -1L) = "add_edit_account?accountId=$accountId"
    }
    object AddEditTransaction : Screen("add_edit_transaction/{accountId}?transactionId={transactionId}") {
        fun createRoute(accountId: Long, transactionId: Long = -1L) = 
            "add_edit_transaction/$accountId?transactionId=$transactionId"
    }
    object AccountList : Screen("account_list")
}
