package com.solanki.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.solanki.myapplication.data.model.Account
import com.solanki.myapplication.data.model.Transaction
import com.solanki.myapplication.data.model.Template

@Database(entities = [Account::class, Transaction::class, Template::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PocketLedgerDatabase : RoomDatabase() {
    abstract val dao: PocketLedgerDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        amount REAL,
                        category TEXT,
                        note TEXT,
                        accountId INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
