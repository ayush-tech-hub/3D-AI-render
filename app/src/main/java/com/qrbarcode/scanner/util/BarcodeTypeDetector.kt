package com.qrbarcode.scanner.util

import com.google.mlkit.vision.barcode.common.Barcode
import com.qrbarcode.scanner.domain.model.ScanType

object BarcodeTypeDetector {

    fun detectType(barcode: Barcode): ScanType = when (barcode.valueType) {
        Barcode.TYPE_URL -> ScanType.URL
        Barcode.TYPE_PHONE -> ScanType.PHONE
        Barcode.TYPE_EMAIL -> ScanType.EMAIL
        Barcode.TYPE_WIFI -> ScanType.WIFI
        Barcode.TYPE_SMS -> ScanType.SMS
        Barcode.TYPE_GEO -> ScanType.GEO
        Barcode.TYPE_CONTACT_INFO -> ScanType.CONTACT
        Barcode.TYPE_CALENDAR_EVENT -> ScanType.CALENDAR
        Barcode.TYPE_ISBN, Barcode.TYPE_PRODUCT -> ScanType.PRODUCT_BARCODE
        else -> ScanType.TEXT
    }

    fun getBarcodeFormat(format: Int): String = when (format) {
        Barcode.FORMAT_QR_CODE -> "QR Code"
        Barcode.FORMAT_EAN_13 -> "EAN-13"
        Barcode.FORMAT_EAN_8 -> "EAN-8"
        Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_UPC_E -> "UPC-E"
        Barcode.FORMAT_CODE_39 -> "Code 39"
        Barcode.FORMAT_CODE_93 -> "Code 93"
        Barcode.FORMAT_CODE_128 -> "Code 128"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_AZTEC -> "Aztec"
        Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
        Barcode.FORMAT_CODABAR -> "Codabar"
        else -> "Barcode"
    }

    fun getDisplayContent(barcode: Barcode): String = when (barcode.valueType) {
        Barcode.TYPE_URL ->
            barcode.url?.url ?: barcode.rawValue ?: ""

        Barcode.TYPE_PHONE ->
            barcode.phone?.number ?: barcode.rawValue ?: ""

        Barcode.TYPE_EMAIL -> buildString {
            barcode.email?.let { email ->
                append(email.address ?: "")
                if (!email.subject.isNullOrEmpty()) append("\nSubject: ${email.subject}")
                if (!email.body.isNullOrEmpty()) append("\nBody: ${email.body}")
            }
        }.ifEmpty { barcode.rawValue ?: "" }

        Barcode.TYPE_WIFI -> buildString {
            barcode.wifi?.let { wifi ->
                append("SSID: ${wifi.ssid ?: ""}")
                if (!wifi.password.isNullOrEmpty()) append("\nPassword: ${wifi.password}")
                append("\nSecurity: ${
                    when (wifi.encryptionType) {
                        Barcode.WiFi.TYPE_WPA -> "WPA/WPA2"
                        Barcode.WiFi.TYPE_WEP -> "WEP"
                        else -> "Open"
                    }
                }")
            }
        }.ifEmpty { barcode.rawValue ?: "" }

        Barcode.TYPE_SMS -> buildString {
            barcode.sms?.let { sms ->
                append("To: ${sms.phoneNumber ?: ""}")
                if (!sms.message.isNullOrEmpty()) append("\nMessage: ${sms.message}")
            }
        }.ifEmpty { barcode.rawValue ?: "" }

        Barcode.TYPE_GEO -> barcode.geoPoint?.let {
            "Latitude: ${it.lat}\nLongitude: ${it.lng}"
        } ?: barcode.rawValue ?: ""

        Barcode.TYPE_CONTACT_INFO -> buildString {
            barcode.contactInfo?.let { contact ->
                contact.name?.formattedName?.let { appendLine(it) }
                contact.organization?.let { appendLine("Org: $it") }
                contact.phones.forEach { appendLine("Phone: ${it.number}") }
                contact.emails.forEach { appendLine("Email: ${it.address}") }
                contact.urls.forEach { appendLine("Web: $it") }
                contact.addresses.forEach { addr ->
                    appendLine("Address: ${addr.addressLines.joinToString(", ")}")
                }
            }
        }.trim().ifEmpty { barcode.rawValue ?: "" }

        Barcode.TYPE_CALENDAR_EVENT -> buildString {
            barcode.calendarEvent?.let { event ->
                event.summary?.let { appendLine("Event: $it") }
                event.start?.let { appendLine("Start: ${it.rawValue}") }
                event.end?.let { appendLine("End: ${it.rawValue}") }
                event.location?.let { appendLine("Location: $it") }
                event.description?.let { appendLine("Description: $it") }
            }
        }.trim().ifEmpty { barcode.rawValue ?: "" }

        else -> barcode.rawValue ?: ""
    }
}
