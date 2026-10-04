package com.example.data.remote.gemini

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class SearchSource(
    val title: String,
    val uri: String
)

data class MapSource(
    val title: String,
    val address: String,
    val uri: String
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val searchSources: List<SearchSource> = emptyList(),
    val mapSources: List<MapSource> = emptyList(),
    val modelUsed: String = ""
)

enum class GeminiModel(
    val modelId: String,
    val displayName: String,
    val badge: String,
    val description: String
) {
    FLASH_35("gemini-3.5-flash", "Gemini 3.5 Flash", "Default", "Balanced, intelligent, supports Live Search & Maps grounding"),
    PRO_31("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "Deep Reasoning", "Best for complex cattle nutrition formulas & deep financial forecasting"),
    FLASH_LITE("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash Lite", "Ultra Fast", "Rapid answers for quick daily lookups"),
    FLASH_38("gemini-3.8-flash", "Gemini 3.8 Flash", "Latest Preview", "Next-gen multimodal & rapid dairy insights")
}

class GeminiService {

    companion object {
        private const val TAG = "GeminiService"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

        const val DEFAULT_SYSTEM_INSTRUCTION = """You are MilkMate AI, an expert Dairy Farm Operations Consultant, Veterinarian Advisor, and Milk Distribution Specialist.
Your goal is to assist dairy farmers, milk plant owners, collection centers, and delivery agents with practical, high-value advice.

Key domains of expertise:
1. Animal Nutrition & Feed Planning: TMR (Total Mixed Ration), green & dry fodder balance, bypass protein, mineral mixtures, silage making, feed conversion ratio (FCR), and milk fat/SNF boosting formulas.
2. Cattle Health & Care: Heat detection, artificial insemination (AI) timing, mastitis prevention, somatic cell counts, vaccination schedules, seasonal care (summer heat stress, winter warming).
3. Milk Testing & Pricing: Gerber fat testing, SNF calculation from CLR (Corrected Lactometer Reading), TS (Total Solids), two-axis pricing charts, adulteration detection.
4. Dairy Farm Economics: Daily break-even analysis, feed cost per liter of milk, profitability per cow/buffalo, and reducing delivery route logistics expenses.
5. Real-Time Market Intelligence: When Google Search or Maps grounding is enabled, utilize live verified data for current milk wholesale rates, feed wholesale prices, government schemes (NABARD, Rashtriya Gokul Mission), and local veterinary/dairy infrastructure.

Be concise, structured, friendly, and practical. Use formatting (bullet points, bold text) for readability."""
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(
        history: List<ChatMessage>,
        userMessage: String,
        model: GeminiModel = GeminiModel.FLASH_35,
        enableSearch: Boolean = false,
        enableMaps: Boolean = false,
        systemInstruction: String = DEFAULT_SYSTEM_INSTRUCTION,
        businessContext: String? = null
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(
                    IllegalStateException("Gemini API key is not configured. Please add your key in the Secrets panel in AI Studio.")
                )
            }

            // Build request JSON
            val requestJson = JSONObject()

            // System instruction
            val combinedSystemPrompt = buildString {
                append(systemInstruction)
                if (!businessContext.isNullOrBlank()) {
                    append("\n\n--- Current Dairy Farm Operational Context ---\n")
                    append(businessContext)
                }
            }

            val sysInstructionObj = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", combinedSystemPrompt) })
                })
            }
            requestJson.put("systemInstruction", sysInstructionObj)

            // Multi-turn conversation contents
            val contentsArray = JSONArray()

            // Append historical messages (limit to last 14 turns for context efficiency)
            val filteredHistory = history.filter { !it.isError && it.content.isNotBlank() }.takeLast(14)
            for (msg in filteredHistory) {
                val role = if (msg.role == "user") "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.content) })
                    })
                })
            }

            // Append latest user message
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", userMessage) })
                })
            })
            requestJson.put("contents", contentsArray)

            // Tools: Google Search and Google Maps Grounding
            val toolsArray = JSONArray()
            if (enableSearch) {
                toolsArray.put(JSONObject().apply {
                    put("googleSearch", JSONObject())
                })
            }
            if (enableMaps) {
                toolsArray.put(JSONObject().apply {
                    put("googleMaps", JSONObject())
                })
            }
            if (toolsArray.length() > 0) {
                requestJson.put("tools", toolsArray)
            }

            // Generation config
            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 2048)
            }
            requestJson.put("generationConfig", genConfig)

            // Execute request
            val targetModel = if (enableSearch || enableMaps) {
                // Grounding tools are best supported on gemini-3.5-flash
                if (model == GeminiModel.FLASH_35 || model == GeminiModel.FLASH_38) model.modelId else "gemini-3.5-flash"
            } else {
                model.modelId
            }

            val endpoint = "$BASE_URL$targetModel:generateContent?key=$apiKey"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(endpoint)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}: $responseBody"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                Log.e(TAG, "Gemini API failed: $errorMsg")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("No response received from Gemini model."))
            }

            val firstCandidate = candidates.getJSONObject(0)
            val contentObj = firstCandidate.optJSONObject("content")
            val partsArray = contentObj?.optJSONArray("parts")

            val replyText = buildString {
                if (partsArray != null) {
                    for (i in 0 until partsArray.length()) {
                        val part = partsArray.getJSONObject(i)
                        val text = part.optString("text")
                        if (text.isNotBlank()) {
                            append(text)
                        }
                    }
                }
            }

            // Extract Grounding Metadata (Search & Maps)
            val searchSources = mutableListOf<SearchSource>()
            val mapSources = mutableListOf<MapSource>()

            val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
            if (groundingMetadata != null) {
                // Web Search Grounding Chunks
                val groundingChunks = groundingMetadata.optJSONArray("groundingChunks")
                if (groundingChunks != null) {
                    for (i in 0 until groundingChunks.length()) {
                        val chunk = groundingChunks.getJSONObject(i)
                        val web = chunk.optJSONObject("web")
                        if (web != null) {
                            val title = web.optString("title", "Web Source")
                            val uri = web.optString("uri", "")
                            if (uri.isNotBlank()) {
                                searchSources.add(SearchSource(title = title, uri = uri))
                            }
                        }
                        val maps = chunk.optJSONObject("maps")
                        if (maps != null) {
                            val title = maps.optString("title", "Map Location")
                            val address = maps.optString("address", "")
                            val uri = maps.optString("uri", "")
                            mapSources.add(MapSource(title = title, address = address, uri = uri))
                        }
                    }
                }

                // Web search queries if available
                val webSearchQueries = groundingMetadata.optJSONArray("webSearchQueries")
                if (webSearchQueries != null && searchSources.isEmpty()) {
                    for (i in 0 until webSearchQueries.length()) {
                        val q = webSearchQueries.getString(i)
                        searchSources.add(SearchSource(title = "Google Search: \"$q\"", uri = "https://www.google.com/search?q=${java.net.URLEncoder.encode(q, "UTF-8")}"))
                    }
                }
            }

            val chatReply = ChatMessage(
                role = "model",
                content = replyText.ifBlank { "I received your query but did not generate a response. Please rephrase." },
                searchSources = searchSources.distinctBy { it.uri },
                mapSources = mapSources.distinctBy { it.title },
                modelUsed = targetModel
            )

            Result.success(chatReply)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini API call", e)
            Result.failure(e)
        }
    }
}
