package com.bille.android.presentation.compiler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bille.android.data.remote.ai.GeminiCompilerRepository
import com.bille.android.domain.model.BilleRule
import com.bille.android.domain.validator.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CompilerUiState {
    object Idle : CompilerUiState()
    object Compiling : CompilerUiState()
    data class ReviewCard(val rule: BilleRule) : CompilerUiState()
    data class Error(val message: String) : CompilerUiState()
    object RuleInstalled : CompilerUiState()
}

@HiltViewModel
class RuleCompilerViewModel @Inject constructor(
    private val geminiRepository: GeminiCompilerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CompilerUiState>(CompilerUiState.Idle)
    val uiState: StateFlow<CompilerUiState> = _uiState.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    fun setApiKey(key: String) {
        _apiKey.value = key
    }

    fun compilePrompt(prompt: String) {
        if (prompt.isBlank()) {
            _uiState.value = CompilerUiState.Error("Prompt cannot be empty")
            return
        }
        _uiState.value = CompilerUiState.Compiling

        viewModelScope.launch {
            when (val result = geminiRepository.compileTextPrompt(_apiKey.value, prompt)) {
                is ValidationResult.Success -> {
                    _uiState.value = CompilerUiState.ReviewCard(result.rule)
                }
                is ValidationResult.Failure -> {
                    _uiState.value = CompilerUiState.Error(result.error)
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = CompilerUiState.Idle
    }
}
