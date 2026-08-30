package com.bille.android.domain.validator

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleSchemaValidatorTest {

    private lateinit var validator: RuleSchemaValidator

    @Before
    fun setUp() {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        validator = RuleSchemaValidator(json)
    }

    @Test
    fun testValidRuleSchemaValidation() {
        val validJson = """
        {
          "task_id": "hvac_cooling_advisory",
          "name": "HVAC Natural Cooling Advisory",
          "cooldown_hours": 4,
          "conditions": {
            "all": [
              {"source": "hvac.ac_status", "operator": "equals", "value": "on"},
              {"source": "hvac.indoor_temp_f", "operator": "between", "value": [68, 72]},
              {"source": "weather.outdoor_temp_f", "operator": "lt", "value": 68},
              {"source": "weather.is_raining", "operator": "equals", "value": false}
            ]
          },
          "action": {
            "title": "HVAC Cooling Advisory",
            "message": "Outdoor temp is pleasant. Turn off AC and open windows!",
            "actions": ["Done", "Snooze"]
          }
        }
        """.trimIndent()

        val result = validator.validate(validJson)
        assertTrue(result is ValidationResult.Success)
        val rule = (result as ValidationResult.Success).rule
        assertEquals("hvac_cooling_advisory", rule.taskId)
        assertEquals(4, rule.conditions.all.size)
    }

    @Test
    fun testInvalidOperatorValidation() {
        val invalidJson = """
        {
          "task_id": "invalid_operator_test",
          "name": "Invalid Operator Test",
          "conditions": {
            "all": [
              {"source": "sensor.temp", "operator": "invalid_op", "value": 100}
            ]
          },
          "action": {
            "title": "Test",
            "message": "Test message"
          }
        }
        """.trimIndent()

        val result = validator.validate(invalidJson)
        assertTrue(result is ValidationResult.Failure)
        val failure = result as ValidationResult.Failure
        assertTrue(failure.error.contains("invalid condition operator"))
    }
}
