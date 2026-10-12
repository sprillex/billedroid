package com.jules.orchestrator.diagnostics

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

object CrashParser {

    const val EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME"
    const val EXTRA_VERSION_NAME = "EXTRA_VERSION_NAME"
    const val EXTRA_STACK_TRACE = "EXTRA_STACK_TRACE"

    private val PACKAGE_REGEX = Regex("(?i)(?:Package|Package Name|App ID):\\s*([a-zA-Z0-9_.]+)")
    private val VERSION_REGEX = Regex("(?i)(?:Version|App Version):\\s*([^\\r\\n]+)")
    private val DEVICE_REGEX = Regex("(?i)(?:Device|Model|Hardware):\\s*([^\\r\\n]+)")
    private val EXCEPTION_REGEX = Regex("(?:java\\.lang\\.\\w+Exception|android\\.\\w+|[a-zA-Z0-9_.]+(?:Exception|Error)):.*")

    fun parse(
        rawContent: String,
        extraPackageName: String? = null,
        extraVersionName: String? = null,
        extraStackTrace: String? = null
    ): CrashReportData {
        val parsedPackage = PACKAGE_REGEX.find(rawContent)?.groupValues?.get(1)?.trim()
        val parsedVersion = VERSION_REGEX.find(rawContent)?.groupValues?.get(1)?.trim()
        val parsedDevice = DEVICE_REGEX.find(rawContent)?.groupValues?.get(1)?.trim()

        val packageName = parsedPackage ?: extraPackageName
        val versionName = parsedVersion ?: extraVersionName

        val stackTrace = extractStackTrace(rawContent) ?: extraStackTrace

        return CrashReportData(
            packageName = packageName,
            versionName = versionName,
            deviceInfo = parsedDevice,
            stackTrace = stackTrace,
            rawContent = rawContent
        )
    }

    fun parseIntent(intent: Intent, contentResolver: ContentResolver? = null): CrashReportData {
        val extraPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
            ?: intent.getStringExtra("extra_package_name")
        val extraVersionName = intent.getStringExtra(EXTRA_VERSION_NAME)
            ?: intent.getStringExtra("extra_version_name")
        val extraStackTrace = intent.getStringExtra(EXTRA_STACK_TRACE)
            ?: intent.getStringExtra("extra_stack_trace")

        var rawContent = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""

        if (rawContent.isBlank()) {
            val streamUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }
            if (streamUri != null && contentResolver != null) {
                try {
                    contentResolver.openInputStream(streamUri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            rawContent = reader.readText()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return parse(
            rawContent = rawContent,
            extraPackageName = extraPackageName,
            extraVersionName = extraVersionName,
            extraStackTrace = extraStackTrace
        )
    }

    private fun extractStackTrace(rawContent: String): String? {
        if (rawContent.isBlank()) return null

        // Stage 1: Explicit Section Split
        if (rawContent.contains("=== STACK TRACE ===")) {
            val parts = rawContent.split("=== STACK TRACE ===", limit = 2)
            if (parts.size > 1 && parts[1].trim().isNotEmpty()) {
                return parts[1].trim()
            }
        }

        // Stage 2: Exception Regex Matching
        val match = EXCEPTION_REGEX.find(rawContent)
        if (match != null) {
            val extracted = rawContent.substring(match.range.start).trim()
            if (extracted.isNotEmpty()) {
                return extracted
            }
        }

        // Stage 3: Raw Text Fallback
        return rawContent.trim().ifEmpty { null }
    }

    fun formatJulesPrompt(data: CrashReportData): String {
        val pkg = data.packageName ?: "Unknown"
        val ver = data.versionName ?: "Unknown"
        val dev = data.deviceInfo ?: "Unknown"
        val stack = data.stackTrace ?: ""

        return """
Fix fatal crash in project: $pkg
Package: $pkg
Version: $ver
Device: $dev

Stack Trace:

$stack

Please analyze the root cause, modify the necessary source files, and commit the fix to a new branch/PR.
""".trim()
    }
}
