package com.jules.orchestrator.diagnostics

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class AppCrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val reportText = buildCrashReport(context, thread, throwable)
            saveCrashReport(context, reportText)
            launchCrashReportActivity(context, reportText)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        const val CRASH_REPORT_FILE = "latest_crash_report.txt"

        fun install(context: Context) {
            val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (currentHandler !is AppCrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(
                    AppCrashHandler(context.applicationContext, currentHandler)
                )
            }
        }

        fun buildCrashReport(context: Context, thread: Thread, throwable: Throwable): String {
            val packageName = context.packageName
            val versionName = getVersionName(context)
            val deviceInfo = getDeviceInfo()
            val threadName = thread.name

            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString().trim()

            return """
                === APP CRASH REPORT ===
                Package: $packageName
                Version: $versionName
                Device: $deviceInfo
                Thread: $threadName

                === STACK TRACE ===
                $stackTrace
            """.trimIndent()
        }

        private fun getVersionName(context: Context): String {
            return try {
                val pInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode.toLong()
                }
                "${pInfo.versionName} ($versionCode)"
            } catch (e: Exception) {
                "Unknown"
            }
        }

        private fun getDeviceInfo(): String {
            val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val model = Build.MODEL
            val sdk = Build.VERSION.SDK_INT
            val release = Build.VERSION.RELEASE
            return "$manufacturer $model (SDK $sdk, Android $release)"
        }

        fun saveCrashReport(context: Context, reportText: String) {
            try {
                val file = File(context.filesDir, CRASH_REPORT_FILE)
                file.writeText(reportText)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun readLatestCrashReport(context: Context): String? {
            return try {
                val file = File(context.filesDir, CRASH_REPORT_FILE)
                if (file.exists()) file.readText() else null
            } catch (e: Exception) {
                null
            }
        }

        private fun launchCrashReportActivity(context: Context, reportText: String) {
            val intent = Intent(context, CrashReportActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(Intent.EXTRA_TEXT, reportText)
            }
            context.startActivity(intent)
        }
    }
}
