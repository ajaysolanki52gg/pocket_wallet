package com.solanki.myapplication.domain.model

import com.solanki.myapplication.data.model.Account

data class AccountWithBalance(
    val account: Account,
    val balance: Double
)
