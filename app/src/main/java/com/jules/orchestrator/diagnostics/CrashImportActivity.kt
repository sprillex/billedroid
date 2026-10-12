package com.jules.orchestrator.diagnostics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

class CrashImportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashData = CrashParser.parseIntent(intent, contentResolver)
        val formattedPrompt = CrashParser.formatJulesPrompt(crashData)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TaskSessionScreen(
                        promptText = formattedPrompt,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}
