package com.qrbarcode.scanner

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.qrbarcode.scanner.ui.navigation.AppNavGraph
import com.qrbarcode.scanner.ui.scanner.ScannerViewModel
import com.qrbarcode.scanner.ui.history.HistoryViewModel
import com.qrbarcode.scanner.ui.theme.QRBarcodeScannerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val PRIVACY_SHOWN_KEY = booleanPreferencesKey("privacy_shown")

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as QRScannerApp
        val showPrivacy = shouldShowPrivacyDialog()

        if (showPrivacy) {
            markPrivacyShown()
        }

        setContent {
            QRBarcodeScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val scannerViewModel: ScannerViewModel = viewModel(
                        factory = ScannerViewModel.Factory(app.repository, showPrivacy)
                    )
                    val historyViewModel: HistoryViewModel = viewModel(
                        factory = HistoryViewModel.Factory(app.repository)
                    )
                    AppNavGraph(
                        navController = navController,
                        scannerViewModel = scannerViewModel,
                        historyViewModel = historyViewModel
                    )
                }
            }
        }
    }

    private fun shouldShowPrivacyDialog(): Boolean = runBlocking {
        val prefs = dataStore.data.first()
        !(prefs[PRIVACY_SHOWN_KEY] ?: false)
    }

    private fun markPrivacyShown() {
        runBlocking {
            dataStore.edit { prefs -> prefs[PRIVACY_SHOWN_KEY] = true }
        }
    }
}
