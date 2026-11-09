package com.example.vpn.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.vpn.model.VpnProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface VpnProfileDao {
    @Query("SELECT * FROM vpn_profiles ORDER BY lastUsedAt DESC, createdAt DESC")
    fun getAllProfiles(): Flow<List<VpnProfile>>

    @Query("SELECT * FROM vpn_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): VpnProfile?

    @Query("SELECT * FROM vpn_profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfile(): VpnProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: VpnProfile)

    @Update
    suspend fun updateProfile(profile: VpnProfile)

    @Delete
    suspend fun deleteProfile(profile: VpnProfile)

    @Query("DELETE FROM vpn_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: String)

    @Query("UPDATE vpn_profiles SET isDefault = 0")
    suspend fun clearDefaultProfiles()

    @Query("UPDATE vpn_profiles SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultProfile(id: String)

    @Query("UPDATE vpn_profiles SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM vpn_profiles")
    suspend fun getProfileCount(): Int
}
