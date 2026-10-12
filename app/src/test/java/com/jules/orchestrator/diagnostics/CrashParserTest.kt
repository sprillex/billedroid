package com.jules.orchestrator.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashParserTest {

    @Test
    fun testParseStandardCanonicalFormat() {
        val rawInput = """
            === APP CRASH REPORT ===
            Package: com.example.targetapp
            Version: 1.4.2 (42)
            Device: Google Pixel 7 (SDK 34, Android 14)
            Thread: main

            === STACK TRACE ===
            java.lang.NullPointerException: Null context in MainActivity
                at com.example.targetapp.MainActivity.onCreate(MainActivity.kt:25)
                at android.app.Activity.performCreate(Activity.java:8595)
        """.trimIndent()

        val data = CrashParser.parse(rawInput)

        assertEquals("com.example.targetapp", data.packageName)
        assertEquals("1.4.2 (42)", data.versionName)
        assertEquals("Google Pixel 7 (SDK 34, Android 14)", data.deviceInfo)
        assertEquals(
            """
            java.lang.NullPointerException: Null context in MainActivity
                at com.example.targetapp.MainActivity.onCreate(MainActivity.kt:25)
                at android.app.Activity.performCreate(Activity.java:8595)
            """.trimIndent(),
            data.stackTrace
        )
    }

    @Test
    fun testParseFlexibleHeaderPatternsAndExceptionRegex() {
        val rawInput = """
            App ID: com.sample.app
            App Version: 2.1.0
            Hardware: Samsung Galaxy S23

            com.sample.app.InitializationException: Failed to initialize database
                at com.sample.app.DatabaseModule.provideDatabase(DatabaseModule.kt:12)
        """.trimIndent()

        val data = CrashParser.parse(rawInput)

        assertEquals("com.sample.app", data.packageName)
        assertEquals("2.1.0", data.versionName)
        assertEquals("Samsung Galaxy S23", data.deviceInfo)
        assertEquals(
            """
            com.sample.app.InitializationException: Failed to initialize database
                at com.sample.app.DatabaseModule.provideDatabase(DatabaseModule.kt:12)
            """.trimIndent(),
            data.stackTrace
        )
    }

    @Test
    fun testParseRawTextFallback() {
        val rawInput = "Just an arbitrary plain text error without structured headers or java exception trace"

        val data = CrashParser.parse(rawInput)

        assertEquals(null, data.packageName)
        assertEquals(null, data.versionName)
        assertEquals(null, data.deviceInfo)
        assertEquals(rawInput, data.stackTrace)
    }

    @Test
    fun testIntentExtrasFallback() {
        val rawInput = """
            Some unstructured text
        """.trimIndent()

        val data = CrashParser.parse(
            rawContent = rawInput,
            extraPackageName = "com.fallback.app",
            extraVersionName = "3.0.0",
            extraStackTrace = "java.lang.IllegalStateException: Explicit extra trace"
        )

        assertEquals("com.fallback.app", data.packageName)
        assertEquals("3.0.0", data.versionName)
        assertEquals("Some unstructured text", data.stackTrace)
    }

    @Test
    fun testFormatJulesPromptWithFullData() {
        val data = CrashReportData(
            packageName = "com.example.targetapp",
            versionName = "1.4.2 (42)",
            deviceInfo = "Google Pixel 7 (SDK 34, Android 14)",
            stackTrace = """
                java.lang.NullPointerException: Null context in MainActivity
                    at com.example.targetapp.MainActivity.onCreate(MainActivity.kt:25)
                    at android.app.Activity.performCreate(Activity.java:8595)
            """.trimIndent(),
            rawContent = ""
        )

        val prompt = CrashParser.formatJulesPrompt(data)

        val expected = """
            Fix fatal crash in project: com.example.targetapp
            Package: com.example.targetapp
            Version: 1.4.2 (42)
            Device: Google Pixel 7 (SDK 34, Android 14)

            Stack Trace:

            java.lang.NullPointerException: Null context in MainActivity
                at com.example.targetapp.MainActivity.onCreate(MainActivity.kt:25)
                at android.app.Activity.performCreate(Activity.java:8595)

            Please analyze the root cause, modify the necessary source files, and commit the fix to a new branch/PR.
        """.trimIndent()

        assertEquals(expected, prompt)
    }

    @Test
    fun testFormatJulesPromptWithMissingMetadata() {
        val data = CrashReportData(
            packageName = null,
            versionName = null,
            deviceInfo = null,
            stackTrace = "java.lang.RuntimeException: Error",
            rawContent = ""
        )

        val prompt = CrashParser.formatJulesPrompt(data)

        assertTrue(prompt.contains("Fix fatal crash in project: Unknown"))
        assertTrue(prompt.contains("Package: Unknown"))
        assertTrue(prompt.contains("Version: Unknown"))
        assertTrue(prompt.contains("Device: Unknown"))
        assertTrue(prompt.contains("java.lang.RuntimeException: Error"))
    }
}
