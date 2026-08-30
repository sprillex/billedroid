package com.bille.android.data.remote.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerialName("system_instruction") val systemInstruction: GeminiSystemInstruction? = null,
    @SerialName("generation_config") val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiSystemInstruction(
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiGenerationConfig(
    @SerialName("response_mime_type") val responseMimeType: String = "application/json",
    val temperature: Float = 0.2f
)

@Serializable
data class GeminiContent(
    val role: String = "user",
    val parts: List<GeminiPart>
)

@Serializable
sealed class GeminiPart {
    @Serializable
    data class TextPart(val text: String) : GeminiPart()

    @Serializable
    data class InlineDataPart(
        @SerialName("inline_data") val inlineData: InlineData
    ) : GeminiPart()
}

@Serializable
data class InlineData(
    @SerialName("mime_type") val mimeType: String,
    val data: String // Base64 encoded bytes
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContentResponse? = null
)

@Serializable
data class GeminiContentResponse(
    val parts: List<GeminiPartResponse> = emptyList()
)

@Serializable
data class GeminiPartResponse(
    val text: String? = null
)
