package com.bille.android.domain.validator

import com.bille.android.domain.model.BilleRule
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

sealed class ValidationResult {
    data class Success(val rule: BilleRule) : ValidationResult()
    data class Failure(val error: String) : ValidationResult()
}

@Singleton
class RuleSchemaValidator @Inject constructor(
    private val json: Json
) {
    companion object {
        val ALLOWED_NAMESPACES = setOf(
            "hvac", "weather", "trash", "aurora", "astronomy", "sensors",
            "calendar", "health", "power", "appliances", "garage", "vehicle"
        )
        val VALID_OPERATORS = setOf(
            "gte", "lte", "gt", "lt", "equals", "eq", "ne", "in", "between", "not_equals", "contains"
        )
    }

    /**
     * Validates raw JSON string output from Gemini or local input.
     * Enforces bill-e schema guardrails: task_id, name, conditions, operators, allowed namespaces, and action format.
     */
    fun validate(rawJson: String): ValidationResult {
        return try {
            val rule = json.decodeFromString<BilleRule>(rawJson)

            if (rule.taskId.isBlank()) {
                return ValidationResult.Failure("task_id cannot be blank")
            }
            if (rule.name.isBlank()) {
                return ValidationResult.Failure("rule name cannot be blank")
            }
            if (rule.conditions.all.isEmpty() && rule.conditions.any.isEmpty()) {
                return ValidationResult.Failure("rule must contain at least one condition in 'all' or 'any'")
            }

            val allConditions = rule.conditions.all + rule.conditions.any
            for (cond in allConditions) {
                if (cond.source.isBlank()) {
                    return ValidationResult.Failure("condition source cannot be blank")
                }
                val namespace = cond.source.trim().substringBefore('.')
                if (namespace.lowercase() !in ALLOWED_NAMESPACES) {
                    return ValidationResult.Failure(
                        "Schema guardrail validation failed: Unauthorized source namespace '$namespace'. Allowed: $ALLOWED_NAMESPACES"
                    )
                }
                if (cond.operator.lowercase() !in VALID_OPERATORS) {
                    return ValidationResult.Failure("invalid condition operator '${cond.operator}'. Allowed: $VALID_OPERATORS")
                }
            }

            if (rule.action.title.isBlank() || rule.action.message.isBlank()) {
                return ValidationResult.Failure("action title and message cannot be blank")
            }

            ValidationResult.Success(rule)
        } catch (e: Exception) {
            ValidationResult.Failure("JSON Schema Parsing Error: ${e.localizedMessage ?: e.message}")
        }
    }
}
