package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScanHistoryEntity): Long

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()
}

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE `key` = :key LIMIT 1")
    fun getConfig(key: String): Flow<AppConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfig(config: AppConfigEntity)
}

@Dao
interface CustomMunicipalityDao {
    @Query("SELECT * FROM custom_municipalities ORDER BY createdAt DESC")
    fun getAllCustomMunicipalities(): Flow<List<CustomMunicipalityEntity>>

    @Query("SELECT * FROM custom_municipalities WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CustomMunicipalityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CustomMunicipalityEntity)

    @Query("DELETE FROM custom_municipalities WHERE id = :id")
    suspend fun deleteById(id: String)
}
