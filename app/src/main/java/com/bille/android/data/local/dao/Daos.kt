package com.bille.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bille.android.data.local.entity.RuleEntity
import com.bille.android.data.local.entity.TriggerHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY installedAtTimestamp DESC")
    fun getAllRules(): Flow<List<RuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity)

    @Query("DELETE FROM rules WHERE taskId = :taskId")
    suspend fun deleteRule(taskId: String)
}

@Dao
interface TriggerHistoryDao {
    @Query("SELECT * FROM trigger_history ORDER BY triggeredAtTimestamp DESC")
    fun getAllHistory(): Flow<List<TriggerHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: TriggerHistoryEntity)
}
