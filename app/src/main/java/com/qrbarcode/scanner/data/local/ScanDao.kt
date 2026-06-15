package com.qrbarcode.scanner.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Query("SELECT * FROM scans ORDER BY isPinned DESC, timestamp DESC")
    fun getAllScans(): Flow<List<ScanEntity>>

    @Query(
        "SELECT * FROM scans " +
        "WHERE content LIKE '%' || :query || '%' " +
        "   OR rawValue LIKE '%' || :query || '%' " +
        "   OR format LIKE '%' || :query || '%' " +
        "ORDER BY isPinned DESC, timestamp DESC"
    )
    fun searchScans(query: String): Flow<List<ScanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity): Long

    @Delete
    suspend fun deleteScan(scan: ScanEntity)

    @Query("DELETE FROM scans")
    suspend fun deleteAllScans()

    @Query("UPDATE scans SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinStatus(id: Long, isPinned: Boolean)

    @Query("SELECT * FROM scans WHERE id = :id LIMIT 1")
    suspend fun getScanById(id: Long): ScanEntity?

    @Query("SELECT * FROM scans ORDER BY isPinned DESC, timestamp DESC")
    suspend fun getAllScansOnce(): List<ScanEntity>
}
