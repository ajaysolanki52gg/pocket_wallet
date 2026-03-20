package com.solanki.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val amount: Double?,
    val category: String?,
    val note: String?,
    val accountId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
