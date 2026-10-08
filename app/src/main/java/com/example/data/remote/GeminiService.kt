package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Decoupled Credentials & Provider Spec passed from the Backend Repository for a specific authenticated User.
 * No global or hardcoded API keys exist anywhere in this class!
 *
 * Architecture: User -> AI Configuration -> Provider -> Model -> AI Task
 */
data class ResolvedUserAiCredential(
    val providerId: String,      // "GEMINI", "OPENAI", "ANTHROPIC", "OPENROUTER", "CUSTOM_OPENAI"
    val providerName: String,
    val modelName: String,
    val baseUrl: String,
    val decryptedApiKey: String
)

@Serializable
data class OcrStructuredBatchResult(
    val detectedLanguage: String = "English",
    val chapters: List<OcrChapterDto> = emptyList()
)

@Serializable
data class OcrChapterDto(
    val chapterTitle: String,
    val chapterSummary: String = "",
    val sections: List<OcrSectionDto> = emptyList()
)

@Serializable
data class OcrSectionDto(
    val sectionHeading: String,
    val paragraphs: List<OcrParagraphDto> = emptyList()
)

@Serializable
data class OcrParagraphDto(
    val pageNumber: Int,
    val blockType: String = "PARAGRAPH",
    val cleanText: String
)

@Serializable
data class SectionTranslationResponseDto(
    val translations: List<ParagraphTranslationItemDto> = emptyList()
)

@Serializable
data class ParagraphTranslationItemDto(
    val paragraphId: Long,
    val translatedText: String,
    val notes: String = ""
)

/**
 * Multi-Provider AI Gateway supporting:
 * - Google Gemini (`generativelanguage.googleapis.com`)
 * - OpenAI (`api.openai.com`)
 * - Anthropic Claude (`api.anthropic.com`)
 * - OpenRouter (`openrouter.ai/api`)
 * - Any Custom OpenAI-Compatible Provider (Groq, Together, DeepSeek, etc.)
 *
 * Security Guarantee:
 * - Never logs API keys or request headers.
 * - Uses only the authenticated user's decrypted credential passed per-call from the backend repository.
 */
object MultiProviderAiGateway {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        isLenient = true
    }

    /**
     * Tests a user's AI provider connection & API key with a lightweight ping prompt.
     */
    suspend fun testConnection(credential: ResolvedUserAiCredential): Result<String> =
        withContext(Dispatchers.IO) {
            if (credential.decryptedApiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("API key cannot be empty."))
            }
            val result = executePrompt(
                credential = credential,
                systemInstruction = "You are a connectivity verification assistant.",
                userPrompt = "Reply with the exact phrase: Connection verified with ${credential.providerName} (${credential.modelName}).",
                imagesBase64 = emptyList(),
                expectJson = false
            )
            result.map { response ->
                response.take(140).trim().ifBlank {
                    "Verified ${credential.providerName} (${credential.modelName})"
                }
            }
        }

    /**
     * Task 1: Structured OCR & Hierarchy Extraction using the user's configured OCR AI Provider.
     */
    suspend fun performStructuredOcrOnPages(
        credential: ResolvedUserAiCredential,
        bookTitle: String,
        pageNumbers: List<Int>,
        pageImagesBase64: List<String>
    ): Result<OcrStructuredBatchResult> = withContext(Dispatchers.IO) {
        val pagesRangeLabel = pageNumbers.joinToString(", ")
        val systemInstruction = """
            You are an expert multilingual OCR & Document Structure Engine supporting all Indian scripts (Devanagari, Gujarati, Bengali, Tamil, Telugu, Kannada, Malayalam, Gurmukhi, Odia, Perso-Arabic/Urdu, Sanskrit) and global languages.
            You are ingesting pages [$pagesRangeLabel] of the document "$bookTitle".
            
            Return ONLY valid JSON matching this exact structure:
            {
              "detectedLanguage": "Sanskrit",
              "chapters": [
                {
                  "chapterTitle": "Chapter Title",
                  "chapterSummary": "Brief summary",
                  "sections": [
                    {
                      "sectionHeading": "Section Heading",
                      "paragraphs": [
                        {
                          "pageNumber": ${pageNumbers.firstOrNull() ?: 1},
                          "blockType": "PARAGRAPH",
                          "cleanText": "Normalized OCR text preserving original script and meaning"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val userPrompt = "Extract and normalize all text from pages [$pagesRangeLabel] of '$bookTitle' into the specified JSON structure."

        val rawResponse = executePrompt(
            credential = credential,
            systemInstruction = systemInstruction,
            userPrompt = userPrompt,
            imagesBase64 = pageImagesBase64,
            expectJson = true
        ).getOrElse { return@withContext Result.failure(it) }

        try {
            val cleanedJson = extractJsonBlock(rawResponse)
            val parsed = json.decodeFromString<OcrStructuredBatchResult>(cleanedJson)
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse OCR JSON from ${credential.providerName}: ${e.localizedMessage}"))
        }
    }

    /**
     * Task 2: Paragraph-Aligned Section Translation using the user's configured Translation AI Provider.
     */
    suspend fun translateStoredParagraphs(
        credential: ResolvedUserAiCredential,
        bookTitle: String,
        chapterTitle: String,
        sectionHeading: String,
        sourceLanguage: String,
        targetLanguage: String,
        paragraphsToTranslate: List<Pair<Long, String>>,
        customInstruction: String? = null
    ): Result<List<ParagraphTranslationItemDto>> = withContext(Dispatchers.IO) {
        val systemText = """
            You are an expert literary, technical, and classical translator specializing in Indian languages (Gujarati, Hindi, Marathi, Bengali, Tamil, Telugu, Kannada, Malayalam, Punjabi, Odia, Assamese, Urdu, Sanskrit) and global languages.
            You are translating stored, OCR-normalized paragraphs from:
            Book: "$bookTitle" | Chapter: "$chapterTitle" | Section: "$sectionHeading"
            Source Language: $sourceLanguage -> Target Language: $targetLanguage.
            
            Return ONLY valid JSON in this exact format:
            {
              "translations": [
                {
                  "paragraphId": 123,
                  "translatedText": "Translated text in $targetLanguage native script",
                  "notes": "Brief key term or transliteration note"
                }
              ]
            }
            ${if (!customInstruction.isNullOrBlank()) "\nAdditional Instruction: $customInstruction" else ""}
        """.trimIndent()

        val userPrompt = buildString {
            appendLine("Translate each paragraph below from $sourceLanguage into $targetLanguage and return the JSON object:")
            paragraphsToTranslate.forEach { (id, text) ->
                appendLine("paragraphId=$id | text: $text")
            }
        }

        val rawResponse = executePrompt(
            credential = credential,
            systemInstruction = systemText,
            userPrompt = userPrompt,
            imagesBase64 = emptyList(),
            expectJson = true
        ).getOrElse { return@withContext Result.failure(it) }

        try {
            val cleanedJson = extractJsonBlock(rawResponse)
            val parsed = json.decodeFromString<SectionTranslationResponseDto>(cleanedJson)
            Result.success(parsed.translations)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse translation JSON from ${credential.providerName}: ${e.localizedMessage}"))
        }
    }

    /**
     * Task 3: Conversational Study & Summarization using the user's configured Chat AI Provider.
     */
    suspend fun chatWithStructuredBook(
        credential: ResolvedUserAiCredential,
        bookTitle: String,
        chapterTitle: String,
        sectionHeading: String,
        sourceLanguage: String,
        targetLanguage: String,
        sectionStructuredText: String,
        userPrompt: String,
        priorHistory: List<Pair<String, String>>
    ): Result<String> = withContext(Dispatchers.IO) {
        val systemText = """
            You are the Polyglot Library AI Scholar powered by the user's configured provider (${credential.providerName} • ${credential.modelName}).
            Book: "$bookTitle" (Source Language: $sourceLanguage).
            Current Chapter: "$chapterTitle" | Current Section: "$sectionHeading".
            User's Preferred Language: **$targetLanguage**.
            Always respond in **$targetLanguage** (using its native script) unless asked for bilingual comparison.
        """.trimIndent()

        val combinedPrompt = buildString {
            if (priorHistory.isNotEmpty()) {
                appendLine("--- Prior Conversation Context ---")
                priorHistory.takeLast(4).forEach { (role, msg) ->
                    appendLine("${role.uppercase()}: $msg")
                }
                appendLine()
            }
            appendLine("--- Current Section Structured Text ($chapterTitle > $sectionHeading) ---")
            appendLine(sectionStructuredText)
            appendLine()
            appendLine("--- User Question (Respond in $targetLanguage) ---")
            appendLine(userPrompt)
        }

        executePrompt(
            credential = credential,
            systemInstruction = systemText,
            userPrompt = combinedPrompt,
            imagesBase64 = emptyList(),
            expectJson = false
        )
    }

    // =====================================================================
    // UNIFIED PROVIDER DISPATCHER (GEMINI / OPENAI / ANTHROPIC / OPENROUTER)
    // =====================================================================

    private fun executePrompt(
        credential: ResolvedUserAiCredential,
        systemInstruction: String,
        userPrompt: String,
        imagesBase64: List<String>,
        expectJson: Boolean
    ): Result<String> {
        return try {
            when (credential.providerId.uppercase()) {
                "GEMINI" -> callGeminiRest(
                    credential = credential,
                    systemInstruction = systemInstruction,
                    userPrompt = userPrompt,
                    imagesBase64 = imagesBase64,
                    expectJson = expectJson
                )
                "ANTHROPIC" -> callAnthropicRest(
                    credential = credential,
                    systemInstruction = systemInstruction,
                    userPrompt = userPrompt,
                    imagesBase64 = imagesBase64
                )
                else -> callOpenAiCompatibleRest(
                    credential = credential,
                    systemInstruction = systemInstruction,
                    userPrompt = userPrompt,
                    imagesBase64 = imagesBase64,
                    expectJson = expectJson
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callGeminiRest(
        credential: ResolvedUserAiCredential,
        systemInstruction: String,
        userPrompt: String,
        imagesBase64: List<String>,
        expectJson: Boolean
    ): Result<String> {
        val base = credential.baseUrl.trimEnd('/')
        val model = credential.modelName.removePrefix("models/")
        val url = "$base/v1beta/models/$model:generateContent?key=${credential.decryptedApiKey}"

        val payload = buildJsonObject {
            putJsonObject("systemInstruction") {
                putJsonArray("parts") {
                    add(buildJsonObject { put("text", systemInstruction) })
                }
            }
            putJsonArray("contents") {
                add(
                    buildJsonObject {
                        put("role", "user")
                        putJsonArray("parts") {
                            imagesBase64.forEach { b64 ->
                                add(
                                    buildJsonObject {
                                        putJsonObject("inlineData") {
                                            put("mimeType", "image/jpeg")
                                            put("data", b64)
                                        }
                                    }
                                )
                            }
                            add(buildJsonObject { put("text", userPrompt) })
                        }
                    }
                )
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.25)
                if (expectJson) {
                    put("responseMimeType", "application/json")
                }
            }
        }

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Gemini API Error (${response.code}): ${extractErrorSummary(bodyStr)}"))
            }
            val root = json.parseToJsonElement(bodyStr).jsonObject
            val text = root["candidates"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray
                ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }
                ?.joinToString("\n")
                ?.trim()

            return if (!text.isNullOrEmpty()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Empty response from Gemini (${credential.modelName})."))
            }
        }
    }

    private fun callOpenAiCompatibleRest(
        credential: ResolvedUserAiCredential,
        systemInstruction: String,
        userPrompt: String,
        imagesBase64: List<String>,
        expectJson: Boolean
    ): Result<String> {
        val base = credential.baseUrl.trimEnd('/')
        val endpoint = if (base.endsWith("/v1")) "$base/chat/completions" else "$base/v1/chat/completions"

        val userContent: JsonElement = if (imagesBase64.isEmpty()) {
            JsonPrimitive(userPrompt)
        } else {
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("type", "text")
                        put("text", userPrompt)
                    }
                )
                imagesBase64.forEach { b64 ->
                    add(
                        buildJsonObject {
                            put("type", "image_url")
                            putJsonObject("image_url") {
                                put("url", "data:image/jpeg;base64,$b64")
                            }
                        }
                    )
                }
            }
        }

        val payload = buildJsonObject {
            put("model", credential.modelName)
            put("temperature", 0.25)
            if (expectJson && credential.providerId.uppercase() == "OPENAI") {
                putJsonObject("response_format") {
                    put("type", "json_object")
                }
            }
            putJsonArray("messages") {
                add(
                    buildJsonObject {
                        put("role", "system")
                        put("content", systemInstruction)
                    }
                )
                add(
                    buildJsonObject {
                        put("role", "user")
                        put("content", userContent)
                    }
                )
            }
        }

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer ${credential.decryptedApiKey}")
            .addHeader("Content-Type", "application/json")

        if (credential.providerId.uppercase() == "OPENROUTER") {
            requestBuilder.addHeader("X-Title", "Polyglot PDF Library")
        }

        val request = requestBuilder
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return Result.failure(Exception("${credential.providerName} API Error (${response.code}): ${extractErrorSummary(bodyStr)}"))
            }
            val root = json.parseToJsonElement(bodyStr).jsonObject
            val text = root["choices"]?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("message")?.jsonObject
                ?.get("content")?.jsonPrimitive?.content
                ?.trim()

            return if (!text.isNullOrEmpty()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Empty response from ${credential.providerName}."))
            }
        }
    }

    private fun callAnthropicRest(
        credential: ResolvedUserAiCredential,
        systemInstruction: String,
        userPrompt: String,
        imagesBase64: List<String>
    ): Result<String> {
        val base = credential.baseUrl.trimEnd('/')
        val endpoint = if (base.endsWith("/v1")) "$base/messages" else "$base/v1/messages"

        val payload = buildJsonObject {
            put("model", credential.modelName)
            put("max_tokens", 4096)
            put("system", systemInstruction)
            putJsonArray("messages") {
                add(
                    buildJsonObject {
                        put("role", "user")
                        putJsonArray("content") {
                            imagesBase64.forEach { b64 ->
                                add(
                                    buildJsonObject {
                                        put("type", "image")
                                        putJsonObject("source") {
                                            put("type", "base64")
                                            put("media_type", "image/jpeg")
                                            put("data", b64)
                                        }
                                    }
                                )
                            }
                            add(
                                buildJsonObject {
                                    put("type", "text")
                                    put("text", userPrompt)
                                }
                            )
                        }
                    }
                )
            }
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("x-api-key", credential.decryptedApiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Anthropic API Error (${response.code}): ${extractErrorSummary(bodyStr)}"))
            }
            val root = json.parseToJsonElement(bodyStr).jsonObject
            val text = root["content"]?.jsonArray
                ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.content }
                ?.joinToString("\n")
                ?.trim()

            return if (!text.isNullOrEmpty()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Empty response from Anthropic (${credential.modelName})."))
            }
        }
    }

    private fun extractJsonBlock(raw: String): String {
        val trimmed = raw.trim()
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        return if (firstBrace >= 0 && lastBrace > firstBrace) {
            trimmed.substring(firstBrace, lastBrace + 1)
        } else {
            trimmed
        }
    }

    private fun extractErrorSummary(bodyStr: String): String {
        return try {
            val obj = json.parseToJsonElement(bodyStr).jsonObject
            val errObj = obj["error"]
            when (errObj) {
                is JsonObject -> errObj["message"]?.jsonPrimitive?.content ?: bodyStr.take(160)
                is JsonPrimitive -> errObj.content
                else -> bodyStr.take(160)
            }
        } catch (_: Exception) {
            bodyStr.take(160)
        }
    }
}
