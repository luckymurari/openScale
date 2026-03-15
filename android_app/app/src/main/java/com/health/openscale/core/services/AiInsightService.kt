/*
 * openScale
 * Copyright (C) 2025 olie.xdev <olie.xdev@googlemail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.health.openscale.core.services

import com.health.openscale.core.data.Insight
import com.health.openscale.core.data.Measurement
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class AiInsightService(
    private val apiKeyProvider: () -> String?
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun generateInsight(
        measurements: List<Measurement>,
        userGoal: String? = null
    ): Result<Insight> {
        val apiKey = apiKeyProvider()
        if (apiKey.isNullOrBlank()) {
            return Result.failure(IllegalStateException("API key not configured"))
        }

        if (measurements.isEmpty()) {
            return Result.failure(IllegalStateException("No measurements available"))
        }

        // Prepare context from last 30 days of data
        val sortedMeasurements = measurements.sortedByDescending { it.timestamp }
        val prompt = buildPrompt(sortedMeasurements, userGoal)

        return try {
            val response = callOpenAI(apiKey, prompt)
            parseInsight(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(measurements: List<Measurement>, userGoal: String?): String {
        val latest = measurements.first()
        val oldest = measurements.last()
        val latestDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(latest.timestamp), ZoneId.systemDefault())
        val oldestDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(oldest.timestamp), ZoneId.systemDefault())
        val daysDiff = ChronoUnit.DAYS.between(oldestDateTime, latestDateTime)

        val prompt = """
You are a health insights assistant analyzing weight tracking data. Provide personalized, actionable insights.

USER DATA:
- Total measurements: ${measurements.size}
- Date range: $daysDiff days
- Latest measurement: ${latestDateTime.toLocalDate()}
- Oldest measurement: ${oldestDateTime.toLocalDate()}
${userGoal?.let { "- User's goal: $it" } ?: ""}

Analyze this data and provide:
1. A brief title for the insight (5-8 words)
2. A detailed message with actionable advice (2-3 sentences)
3. Trend assessment: "improving", "stable", or "needs_attention"
4. Confidence level (0-100) based on data quality

Respond ONLY with valid JSON in this exact format:
{"title":"...", "message":"...", "trend":"...", "confidence":##}
""".trimIndent()

        return prompt
    }

    private fun callOpenAI(apiKey: String, prompt: String): String {
        val requestBody = json.encodeToString(
            serializer = OpenAIRequest.serializer(),
            value = OpenAIRequest(
                model = "gpt-4o-mini",
                messages = listOf(
                    OpenAIMessage(role = "user", content = prompt)
                ),
                max_tokens = 500,
                temperature = 0.7
            )
        )

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw RuntimeException("OpenAI API error: ${response.code} - $errorBody")
            }
            
            val responseBody = response.body?.string()
                ?: throw RuntimeException("Empty response body")
            
            val openAIResponse = json.decodeFromString<OpenAIResponse>(responseBody)
            return openAIResponse.choices.firstOrNull()?.message?.content
                ?: throw RuntimeException("No content in response")
        }
    }

    private fun parseInsight(response: String): Result<Insight> {
        return try {
            // Extract JSON from response (handle potential markdown code blocks)
            val jsonContent = response
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val insightData = json.decodeFromString<InsightData>(jsonContent)
            
            Result.success(
                Insight(
                    id = java.util.UUID.randomUUID().toString(),
                    title = insightData.title,
                    message = insightData.message,
                    trend = insightData.trend,
                    confidence = insightData.confidence.coerceIn(0, 100),
                    createdAt = Instant.now()
                )
            )
        } catch (e: Exception) {
            Result.failure(RuntimeException("Failed to parse AI response: ${e.message}\nResponse: $response"))
        }
    }

    @Serializable
    data class OpenAIRequest(
        val model: String,
        val messages: List<OpenAIMessage>,
        val max_tokens: Int,
        val temperature: Double
    )

    @Serializable
    data class OpenAIMessage(
        val role: String,
        val content: String
    )

    @Serializable
    data class OpenAIResponse(
        val choices: List<OpenAIChoice>
    )

    @Serializable
    data class OpenAIChoice(
        val message: OpenAIMessage
    )

    @Serializable
    data class InsightData(
        val title: String,
        val message: String,
        val trend: String,
        val confidence: Int
    )
}
