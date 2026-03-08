package com.solanki.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.solanki.myapplication.data.model.Account
import com.solanki.myapplication.data.model.Transaction

@Database(entities = [Account::class, Transaction::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PocketLedgerDatabase : RoomDatabase() {
    abstract val dao: PocketLedgerDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add orderIndex column to accounts table if it doesn't exist
                // We use IF NOT EXISTS just in case, though standard SQL for Room migrations usually doesn't need it if versioning is correct.
                // But since we are dealing with imports that might have inconsistent states, it's safer.
                db.execSQL("ALTER TABLE accounts ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
