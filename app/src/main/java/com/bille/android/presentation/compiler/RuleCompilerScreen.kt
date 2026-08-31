package com.bille.android.presentation.compiler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bille.android.domain.model.BilleRule
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun RuleCompilerScreen(
    viewModel: RuleCompilerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    var promptText by remember { mutableStateOf("") }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText: String? =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (spokenText != null) {
                promptText = spokenText
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "AI Rule Compiler",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { viewModel.setApiKey(it) },
            label = { Text("Gemini API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = promptText,
            onValueChange = { promptText = it },
            label = { Text("Describe the rule you want bill-e to execute...") },
            placeholder = { Text("e.g. Alert me when outdoor temp is below 68°F so I can turn off AC and open windows.") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            trailingIcon = {
                IconButton(onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                    }
                    try {
                        speechRecognizerLauncher.launch(intent)
                    } catch (e: android.content.ActivityNotFoundException) {
                        // Ignore if STT is not supported on the device
                    }
                }) {
                    Text("🎤")
                }
            }
        )

        Button(
            onClick = { viewModel.compilePrompt(promptText) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState !is CompilerUiState.Compiling
        ) {
            if (uiState is CompilerUiState.Compiling) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compiling Rule...")
            } else {
                Text("Compile Rule with Gemini")
            }
        }

        when (val state = uiState) {
            is CompilerUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is CompilerUiState.ReviewCard -> {
                RuleReviewCard(
                    rule = state.rule,
                    onApproveAndSign = { viewModel.approveAndSignRule(state.rule) },
                    onReject = { viewModel.resetState() }
                )
            }
            is CompilerUiState.RuleInstalled -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Rule successfully signed & installed on bill-e daemon!",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
fun RuleReviewCard(
    rule: BilleRule,
    onApproveAndSign: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Proposed Action Review",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(text = "Name: ${rule.name}", fontWeight = FontWeight.SemiBold)
            Text(text = "Task ID: ${rule.taskId}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Cooldown: ${rule.cooldownHours} hour(s)", style = MaterialTheme.typography.bodyMedium)

            Text(
                text = "Conditions:",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )
            rule.conditions.all.forEach { cond ->
                Text(
                    text = " • ALL: ${cond.source} ${cond.operator} ${cond.value}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            rule.conditions.any.forEach { cond ->
                Text(
                    text = " • ANY: ${cond.source} ${cond.operator} ${cond.value}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Notification Action:",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(text = "Title: ${rule.action.title}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Message: ${rule.action.message}", style = MaterialTheme.typography.bodyMedium)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = onReject) {
                    Text("Reject")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    var currentContext = context
                    while (currentContext is android.content.ContextWrapper) {
                        if (currentContext is FragmentActivity) break
                        currentContext = currentContext.baseContext
                    }

                    val fragmentActivity = currentContext as? FragmentActivity
                    if (fragmentActivity != null) {
                        val executor = ContextCompat.getMainExecutor(context)
                        val biometricPrompt = BiometricPrompt(
                            fragmentActivity,
                            executor,
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(
                                    result: BiometricPrompt.AuthenticationResult
                                ) {
                                    super.onAuthenticationSucceeded(result)
                                    onApproveAndSign()
                                }
                            }
                        )
                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Sign Rule Intent")
                            .setSubtitle("Authenticate to cryptographically sign payload")
                            .setNegativeButtonText("Cancel")
                            .build()
                        biometricPrompt.authenticate(promptInfo)
                    } else {
                        // Fallback if activity is not a FragmentActivity
                        onApproveAndSign()
                    }
                }) {
                    Text("Approve & Cryptographically Sign")
                }
            }
        }
    }
}
