package com.bille.android.di

import android.content.Context
import androidx.room.Room
import com.bille.android.data.local.BilleDatabase
import com.bille.android.data.local.dao.RuleDao
import com.bille.android.data.local.dao.TriggerHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): BilleDatabase {
        return Room.databaseBuilder(
            context,
            BilleDatabase::class.java,
            "bille.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideRuleDao(database: BilleDatabase): RuleDao = database.ruleDao()

    @Provides
    fun provideTriggerHistoryDao(database: BilleDatabase): TriggerHistoryDao = database.triggerHistoryDao()
}
