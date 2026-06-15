package com.qrbarcode.scanner.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qrbarcode.scanner.domain.model.ScanItem
import com.qrbarcode.scanner.domain.model.ScanType
import com.qrbarcode.scanner.util.ClipboardHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultBottomSheet(
    scan: ScanItem,
    onDismiss: () -> Unit,
    onScanAgain: () -> Unit
) {
    val context = LocalContext.current
    var showUrlDialog by remember { mutableStateOf(false) }
    var showCopiedSnackbar by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type icon + format chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = scan.type.icon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = scan.type.label(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(scan.format, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Content card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Text(
                    text = scan.content,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = if (scan.type == ScanType.TEXT || scan.type == ScanType.PRODUCT_BARCODE)
                            FontFamily.Monospace else FontFamily.Default
                    ),
                    textAlign = TextAlign.Start
                )
            }

            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Copy
                ResultActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy to Clipboard",
                    onClick = {
                        ClipboardHelper.copy(context, scan.rawValue)
                        showCopiedSnackbar = true
                    }
                )

                // Share
                ResultActionButton(
                    icon = Icons.Default.Share,
                    label = "Share",
                    onClick = {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, scan.rawValue)
                                },
                                "Share scan result"
                            )
                        )
                    }
                )

                // Type-specific actions
                when (scan.type) {
                    ScanType.URL -> ResultActionButton(
                        icon = Icons.Default.OpenInBrowser,
                        label = "Open in Browser",
                        isPrimary = true,
                        onClick = { showUrlDialog = true }
                    )
                    ScanType.PHONE -> ResultActionButton(
                        icon = Icons.Default.Call,
                        label = "Call ${scan.content}",
                        isPrimary = true,
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${scan.content}"))
                            context.startActivity(intent)
                        }
                    )
                    ScanType.EMAIL -> ResultActionButton(
                        icon = Icons.Default.Email,
                        label = "Send Email",
                        isPrimary = true,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${scan.rawValue}"))
                            context.startActivity(intent)
                        }
                    )
                    ScanType.SMS -> ResultActionButton(
                        icon = Icons.Default.Sms,
                        label = "Send SMS",
                        isPrimary = true,
                        onClick = {
                            val number = scan.rawValue.substringAfter("smsto:").substringBefore(":")
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:$number")))
                        }
                    )
                    ScanType.GEO -> ResultActionButton(
                        icon = Icons.Default.Map,
                        label = "Open in Maps",
                        isPrimary = true,
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(scan.rawValue)))
                        }
                    )
                    ScanType.WIFI -> ResultActionButton(
                        icon = Icons.Default.Wifi,
                        label = "View Wi-Fi Details",
                        isPrimary = false,
                        onClick = {}
                    )
                    ScanType.CONTACT -> ResultActionButton(
                        icon = Icons.Default.PersonAdd,
                        label = "Add to Contacts",
                        isPrimary = true,
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_INSERT).apply {
                                    type = ContactsContract.Contacts.CONTENT_TYPE
                                }
                            )
                        }
                    )
                    else -> {}
                }
            }

            // Scan again button
            Button(
                onClick = onScanAgain,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scan Again")
            }

            if (showCopiedSnackbar) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showCopiedSnackbar = false
                }
                Snackbar(
                    modifier = Modifier.padding(bottom = 8.dp),
                    action = {
                        TextButton(onClick = { showCopiedSnackbar = false }) { Text("OK") }
                    }
                ) {
                    Text("Copied to clipboard")
                }
            }
        }
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            icon = { Icon(Icons.Default.OpenInBrowser, contentDescription = null) },
            title = { Text("Open URL?") },
            text = { Text(scan.content) },
            confirmButton = {
                TextButton(onClick = {
                    showUrlDialog = false
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(scan.content)))
                }) { Text("Open") }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ResultActionButton(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    if (isPrimary) {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    }
}

fun ScanType.icon(): ImageVector = when (this) {
    ScanType.URL -> Icons.Default.Link
    ScanType.PHONE -> Icons.Default.Phone
    ScanType.EMAIL -> Icons.Default.Email
    ScanType.WIFI -> Icons.Default.Wifi
    ScanType.SMS -> Icons.Default.Sms
    ScanType.GEO -> Icons.Default.LocationOn
    ScanType.CONTACT -> Icons.Default.Person
    ScanType.CALENDAR -> Icons.Default.CalendarToday
    ScanType.PRODUCT_BARCODE -> Icons.Default.ShoppingCart
    ScanType.TEXT -> Icons.Default.TextFields
}

fun ScanType.label(): String = when (this) {
    ScanType.URL -> "URL"
    ScanType.PHONE -> "Phone Number"
    ScanType.EMAIL -> "Email Address"
    ScanType.WIFI -> "Wi-Fi Network"
    ScanType.SMS -> "SMS Message"
    ScanType.GEO -> "Location"
    ScanType.CONTACT -> "Contact"
    ScanType.CALENDAR -> "Calendar Event"
    ScanType.PRODUCT_BARCODE -> "Product Barcode"
    ScanType.TEXT -> "Text"
}
