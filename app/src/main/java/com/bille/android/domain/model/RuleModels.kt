package com.bille.android.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class BilleRule(
    @SerialName("task_id") val taskId: String,
    val name: String,
    @SerialName("cooldown_hours") val cooldownHours: Int = 1,
    val conditions: RuleConditions,
    val action: RuleAction
)

@Serializable
data class RuleConditions(
    val all: List<RuleCondition> = emptyList(),
    val any: List<RuleCondition> = emptyList()
)

@Serializable
data class RuleCondition(
    val source: String,
    val operator: String,
    @Serializable(with = ConditionValueSerializer::class)
    val value: ConditionValue
)

sealed class ConditionValue {
    data class StringValue(val value: String) : ConditionValue()
    data class NumberValue(val value: Double) : ConditionValue()
    data class BooleanValue(val value: Boolean) : ConditionValue()
    data class RangeValue(val min: Double, val max: Double) : ConditionValue()
    data class ListValue(val items: List<String>) : ConditionValue()
}

object ConditionValueSerializer : KSerializer<ConditionValue> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): ConditionValue {
        val input = decoder as? JsonDecoder
            ?: throw IllegalStateException("This serializer can be used only with JsonDecoder")
        val element = input.decodeJsonElement()

        return when {
            element is JsonPrimitive -> {
                val prim = element.jsonPrimitive
                if (prim.isString) {
                    ConditionValue.StringValue(prim.content)
                } else {
                    val boolVal = prim.booleanOrNull
                    if (boolVal != null && (prim.content == "true" || prim.content == "false")) {
                        ConditionValue.BooleanValue(boolVal)
                    } else {
                        val numVal = prim.doubleOrNull
                        if (numVal != null) {
                            ConditionValue.NumberValue(numVal)
                        } else {
                            ConditionValue.StringValue(prim.content)
                        }
                    }
                }
            }
            element is JsonArray -> {
                val array = element.jsonArray
                if (array.size == 2 && array.all { it is JsonPrimitive && (it.jsonPrimitive.doubleOrNull != null) }) {
                    val min = array[0].jsonPrimitive.doubleOrNull!!
                    val max = array[1].jsonPrimitive.doubleOrNull!!
                    ConditionValue.RangeValue(min, max)
                } else {
                    val list = array.map { it.jsonPrimitive.content }
                    ConditionValue.ListValue(list)
                }
            }
            else -> throw IllegalArgumentException("Unsupported condition value element: $element")
        }
    }

    override fun serialize(encoder: Encoder, value: ConditionValue) {
        val output = encoder as? JsonEncoder
            ?: throw IllegalStateException("This serializer can be used only with JsonEncoder")

        val element = when (value) {
            is ConditionValue.StringValue -> JsonPrimitive(value.value)
            is ConditionValue.NumberValue -> JsonPrimitive(value.value)
            is ConditionValue.BooleanValue -> JsonPrimitive(value.value)
            is ConditionValue.RangeValue -> JsonArray(listOf(JsonPrimitive(value.min), JsonPrimitive(value.max)))
            is ConditionValue.ListValue -> JsonArray(value.items.map { JsonPrimitive(it) })
        }
        output.encodeJsonElement(element)
    }
}

@Serializable
data class RuleAction(
    val title: String,
    val message: String,
    val actions: List<String> = listOf("Done", "Snooze")
)
