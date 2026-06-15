package com.qrbarcode.scanner.domain.model

data class ScanItem(
    val id: Long = 0,
    val content: String,
    val rawValue: String,
    val format: String,
    val type: ScanType,
    val timestamp: Long,
    val isPinned: Boolean = false
)
