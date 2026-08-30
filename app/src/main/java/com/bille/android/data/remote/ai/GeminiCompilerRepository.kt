package com.bille.android.data.remote.ai

import com.bille.android.domain.validator.RuleSchemaValidator
import com.bille.android.domain.validator.ValidationResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiCompilerRepository @Inject constructor(
    private val geminiApiService: GeminiApiService,
    private val validator: RuleSchemaValidator
) {
    companion object {
        const val SYSTEM_PROMPT = """
You are the bill-e AI Assistant Compiler. Your job is to compile natural language user requests into a valid bill-e rule JSON object.

Strict JSON Output Format:
{
  "task_id": "unique_snake_case_id",
  "name": "Human readable rule title",
  "cooldown_hours": 4,
  "conditions": {
    "all": [
      {
        "source": "category.attribute_name",
        "operator": "equals|not_equals|gt|gte|lt|lte|between|in|contains",
        "value": "string or number or boolean or [min, max]"
      }
    ],
    "any": []
  },
  "action": {
    "title": "Short Notification Title",
    "message": "Actionable message string for the user",
    "actions": ["Done", "Snooze"]
  }
}

Rules & Constraints:
1. Output ONLY valid JSON matching the exact schema above. No markdown formatting outside of JSON.
2. Operator must be one of: equals, not_equals, gt, gte, lt, lte, between, in, contains.
3. Keep task_id short, unique, and snake_case.
"""
    }

    suspend fun compileTextPrompt(apiKey: String, userPrompt: String): ValidationResult {
        if (apiKey.isBlank()) {
            return ValidationResult.Failure("Gemini API key is required. Please set it in Settings.")
        }
        val request = GeminiRequest(
            systemInstruction = GeminiSystemInstruction(
                parts = listOf(GeminiPart.TextPart(SYSTEM_PROMPT))
            ),
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart.TextPart(userPrompt))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        return try {
            val response = geminiApiService.generateContent(apiKey, request)
            val jsonText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return ValidationResult.Failure("Empty response from Gemini model")

            validator.validate(jsonText)
        } catch (e: Exception) {
            ValidationResult.Failure("Gemini API Request failed: ${e.localizedMessage ?: e.message}")
        }
    }
}
