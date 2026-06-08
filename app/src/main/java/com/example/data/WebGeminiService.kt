package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiReq(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)

interface WebGeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiReq
    ): GeminiResponse
}

@JsonClass(generateAdapter = true)
data class CopilotReply(
    val explanation: String,
    val targetFile: String?, // e.g. "index.html", null if no code generated
    val originalBlockToReplace: String?, // what to look for
    val replacementContent: String? // what to replace it with
)

object RetrofitWebClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiService: WebGeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(WebGeminiApiService::class.java)
    }

    // Safely parses the output from Gemini if requested in JSON
    fun parseCopilotReply(rawJson: String): CopilotReply? {
        val cleaned = cleanJson(rawJson)
        return try {
            val adapter = moshi.adapter(CopilotReply::class.java)
            adapter.fromJson(cleaned)
        } catch (e: Exception) {
            e.printStackTrace()
            // If it can't parse as JSON, generate a nice text-only fallback reply
            CopilotReply(
                explanation = rawJson,
                targetFile = null,
                originalBlockToReplace = null,
                replacementContent = null
            )
        }
    }

    private fun cleanJson(rawStr: String): String {
        var str = rawStr.trim()
        if (str.startsWith("```")) {
            str = str.substringAfter("```")
            if (str.startsWith("json", ignoreCase = true)) {
                str = str.substring(4)
            }
            str = str.substringBeforeLast("```")
        }
        str = str.trim()
        val firstBrace = str.indexOf('{')
        val lastBrace = str.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            str = str.substring(firstBrace, lastBrace + 1)
        }
        return str
    }
}
