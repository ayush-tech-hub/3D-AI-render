package com.qrbarcode.scanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.qrbarcode.scanner.domain.model.ScanItem
import com.qrbarcode.scanner.domain.model.ScanType

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val rawValue: String,
    val format: String,
    val type: String,
    val timestamp: Long,
    val isPinned: Boolean = false
)

fun ScanEntity.toDomain() = ScanItem(
    id = id,
    content = content,
    rawValue = rawValue,
    format = format,
    type = runCatching { ScanType.valueOf(type) }.getOrDefault(ScanType.TEXT),
    timestamp = timestamp,
    isPinned = isPinned
)

fun ScanItem.toEntity() = ScanEntity(
    id = id,
    content = content,
    rawValue = rawValue,
    format = format,
    type = type.name,
    timestamp = timestamp,
    isPinned = isPinned
)
