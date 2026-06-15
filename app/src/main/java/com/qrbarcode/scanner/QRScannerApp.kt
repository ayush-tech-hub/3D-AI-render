package com.qrbarcode.scanner

import android.app.Application
import com.qrbarcode.scanner.data.local.AppDatabase
import com.qrbarcode.scanner.data.repository.ScanRepository
import com.qrbarcode.scanner.data.repository.ScanRepositoryImpl

class QRScannerApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: ScanRepository by lazy { ScanRepositoryImpl(database.scanDao()) }
}
