package com.jules.orchestrator.diagnostics

data class CrashReportData(
    val packageName: String? = null,
    val versionName: String? = null,
    val deviceInfo: String? = null,
    val stackTrace: String? = null,
    val rawContent: String = ""
)
