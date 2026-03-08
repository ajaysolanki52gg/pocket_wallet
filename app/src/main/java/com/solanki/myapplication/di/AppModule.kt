package com.solanki.myapplication.di

import android.app.Application
import androidx.room.Room
import com.solanki.myapplication.data.local.PocketLedgerDao
import com.solanki.myapplication.data.local.PocketLedgerDatabase
import com.solanki.myapplication.data.repository.PocketLedgerRepositoryImpl
import com.solanki.myapplication.domain.repository.PocketLedgerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePocketLedgerDatabase(app: Application): PocketLedgerDatabase {
        return Room.databaseBuilder(
            app,
            PocketLedgerDatabase::class.java,
            "pocket_ledger_db"
        )
        .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE) // Disables WAL and uses a simple journal file for maximum backup compatibility
        .addMigrations(PocketLedgerDatabase.MIGRATION_1_2)
        .build()
    }

    @Provides
    @Singleton
    fun providePocketLedgerDao(db: PocketLedgerDatabase): PocketLedgerDao {
        return db.dao
    }

    @Provides
    @Singleton
    fun providePocketLedgerRepository(dao: PocketLedgerDao, db: PocketLedgerDatabase): PocketLedgerRepository {
        return PocketLedgerRepositoryImpl(dao, db)
    }
}
