package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.tools.DeviceToolsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class GeminiResult {
    data class Success(
        val text: String,
        val functionCalls: List<GeminiToolCall> = emptyList()
    ) : GeminiResult()

    data class Error(val message: String) : GeminiResult()
}

data class GeminiToolCall(
    val name: String,
    val args: Map<String, Any?>
)

data class ChatMessage(
    val role: String, // "user", "model", or "function"
    val text: String? = null,
    val functionCallName: String? = null,
    val functionCallArgs: Map<String, Any?>? = null,
    val functionResponseName: String? = null,
    val functionResponseData: Map<String, Any?>? = null
)

class GeminiClient(
    private var customApiKey: String? = null
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun setCustomApiKey(key: String?) {
        customApiKey = key?.trim()
    }

    fun getEffectiveApiKey(): String {
        val custom = customApiKey
        if (!custom.isNullOrBlank()) return custom
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
    }

    suspend fun generateContent(
        history: List<ChatMessage>,
        systemPrompt: String
    ): GeminiResult = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResult.Error("NO_API_KEY")
        }

        try {
            val rootJson = JSONObject()

            // System Instruction
            val sysInstructionObj = JSONObject()
            val sysPartsArr = JSONArray()
            sysPartsArr.put(JSONObject().put("text", systemPrompt))
            sysInstructionObj.put("parts", sysPartsArr)
            rootJson.put("systemInstruction", sysInstructionObj)

            // Contents Array
            val contentsArr = JSONArray()
            for (msg in history) {
                val contentObj = JSONObject()
                contentObj.put("role", msg.role)
                val partsArr = JSONArray()

                if (!msg.text.isNullOrBlank()) {
                    partsArr.put(JSONObject().put("text", msg.text))
                }

                if (msg.functionCallName != null) {
                    val fcObj = JSONObject()
                    fcObj.put("name", msg.functionCallName)
                    val argsObj = JSONObject()
                    msg.functionCallArgs?.forEach { (k, v) -> argsObj.put(k, v) }
                    fcObj.put("args", argsObj)
                    partsArr.put(JSONObject().put("functionCall", fcObj))
                }

                if (msg.functionResponseName != null) {
                    val frObj = JSONObject()
                    frObj.put("name", msg.functionResponseName)
                    val respObj = JSONObject()
                    msg.functionResponseData?.forEach { (k, v) -> respObj.put(k, v) }
                    frObj.put("response", respObj)
                    partsArr.put(JSONObject().put("functionResponse", frObj))
                }

                contentObj.put("parts", partsArr)
                contentsArr.put(contentObj)
            }
            rootJson.put("contents", contentsArr)

            // Tools (Function Declarations)
            val toolsArr = JSONArray()
            val toolDefObj = JSONObject()
            val fnDeclsArr = JSONArray()
            for (toolMap in DeviceToolsManager.getToolDeclarations()) {
                fnDeclsArr.put(JSONObject(toolMap))
            }
            toolDefObj.put("functionDeclarations", fnDeclsArr)
            toolsArr.put(toolDefObj)
            rootJson.put("tools", toolsArr)

            // Generation Config
            val genConfigObj = JSONObject()
            genConfigObj.put("temperature", 0.9)
            genConfigObj.put("topP", 0.95)
            genConfigObj.put("topK", 40)
            genConfigObj.put("maxOutputTokens", 512)
            rootJson.put("generationConfig", genConfigObj)

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(endpoint)
                .post(rootJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiClient", "API Error ${response.code}: $responseBody")
                return@withContext GeminiResult.Error("API Error ${response.code}")
            }

            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext GeminiResult.Error("No response candidates")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            val functionCalls = mutableListOf<GeminiToolCall>()
            val textBuilder = StringBuilder()

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        textBuilder.append(part.getString("text"))
                    }
                    if (part.has("functionCall")) {
                        val fc = part.getJSONObject("functionCall")
                        val name = fc.getString("name")
                        val argsMap = mutableMapOf<String, Any?>()
                        val argsObj = fc.optJSONObject("args")
                        if (argsObj != null) {
                            val keys = argsObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                argsMap[key] = argsObj.get(key)
                            }
                        }
                        functionCalls.add(GeminiToolCall(name = name, args = argsMap))
                    }
                }
            }

            GeminiResult.Success(
                text = textBuilder.toString().trim(),
                functionCalls = functionCalls
            )
        } catch (e: Exception) {
            Log.e("GeminiClient", "Request Exception", e)
            GeminiResult.Error(e.message ?: "Network request failed")
        }
    }
}
