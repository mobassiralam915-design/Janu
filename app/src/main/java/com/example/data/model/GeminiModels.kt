package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class GeminiRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val tools: List<ToolDefinition>? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = false)
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@JsonClass(generateAdapter = false)
data class Part(
    val text: String? = null,
    val functionCall: FunctionCall? = null,
    val functionResponse: FunctionResponse? = null
)

@JsonClass(generateAdapter = false)
data class FunctionCall(
    val name: String,
    val args: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = false)
data class FunctionResponse(
    val name: String,
    val response: Map<String, Any?>
)

@JsonClass(generateAdapter = false)
data class ToolDefinition(
    val functionDeclarations: List<FunctionDeclaration>
)

@JsonClass(generateAdapter = false)
data class FunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = false)
data class GenerationConfig(
    val temperature: Float = 0.85f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val maxOutputTokens: Int = 1024
)

@JsonClass(generateAdapter = false)
data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val error: GeminiError? = null
)

@JsonClass(generateAdapter = false)
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

@JsonClass(generateAdapter = false)
data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)
