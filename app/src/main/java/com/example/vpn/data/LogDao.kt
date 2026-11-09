package com.example.vpn.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.vpn.model.LogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 300): Flow<List<LogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntry)

    @Query("DELETE FROM connection_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM connection_logs")
    suspend fun getLogCount(): Int
}
