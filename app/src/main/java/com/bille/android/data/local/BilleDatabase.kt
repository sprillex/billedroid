package com.bille.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bille.android.data.local.dao.RuleDao
import com.bille.android.data.local.dao.TriggerHistoryDao
import com.bille.android.data.local.entity.RuleEntity
import com.bille.android.data.local.entity.TriggerHistoryEntity

@Database(
    entities = [RuleEntity::class, TriggerHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BilleDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun triggerHistoryDao(): TriggerHistoryDao
}
