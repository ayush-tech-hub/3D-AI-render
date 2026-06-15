package com.qrbarcode.scanner.data.repository

import com.qrbarcode.scanner.domain.model.ScanItem
import kotlinx.coroutines.flow.Flow

interface ScanRepository {
    fun getAllScans(): Flow<List<ScanItem>>
    fun searchScans(query: String): Flow<List<ScanItem>>
    suspend fun insertScan(scan: ScanItem): Long
    suspend fun deleteScan(scan: ScanItem)
    suspend fun deleteAllScans()
    suspend fun togglePin(id: Long, isPinned: Boolean)
    suspend fun getAllScansOnce(): List<ScanItem>
}
