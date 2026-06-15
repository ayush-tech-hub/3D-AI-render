package com.qrbarcode.scanner.data.repository

import com.qrbarcode.scanner.data.local.ScanDao
import com.qrbarcode.scanner.data.local.toDomain
import com.qrbarcode.scanner.data.local.toEntity
import com.qrbarcode.scanner.domain.model.ScanItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScanRepositoryImpl(private val dao: ScanDao) : ScanRepository {

    override fun getAllScans(): Flow<List<ScanItem>> =
        dao.getAllScans().map { it.map(::toDomainItem) }

    override fun searchScans(query: String): Flow<List<ScanItem>> =
        dao.searchScans(query).map { it.map(::toDomainItem) }

    override suspend fun insertScan(scan: ScanItem): Long =
        dao.insertScan(scan.toEntity())

    override suspend fun deleteScan(scan: ScanItem) =
        dao.deleteScan(scan.toEntity())

    override suspend fun deleteAllScans() =
        dao.deleteAllScans()

    override suspend fun togglePin(id: Long, isPinned: Boolean) =
        dao.updatePinStatus(id, isPinned)

    override suspend fun getAllScansOnce(): List<ScanItem> =
        dao.getAllScansOnce().map(::toDomainItem)

    private fun toDomainItem(entity: com.qrbarcode.scanner.data.local.ScanEntity) = entity.toDomain()
}
