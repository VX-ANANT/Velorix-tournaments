package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
private data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@Serializable
private data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@Serializable
private data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

@Serializable
private data class GeminiThinkingConfig(
    val thinkingLevel: String
)

@Serializable
private data class GeminiGenerationConfig(
    val thinkingConfig: GeminiThinkingConfig? = null,
    val temperature: Float? = null
)

@Serializable
private data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

object GeminiSupportService {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = false }
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun buildSystemPrompt(userContextSummary: String?): String {
        val base = """
            You are Velorix AI, the official support and verification assistant ONLY for the Velorix Esports Platform.

            STRICT DOMAIN SCOPE & GUARDRAILS:
            - You ONLY answer questions regarding Velorix Esports, Free Fire / BGMI tournaments, wallet transactions, match rules, room credentials, and account verification.
            - If the user asks general off-topic questions (e.g., general trivia, math homework, cooking recipes, general software coding, general news), politely decline with: "I am Velorix AI, dedicated exclusively to assisting you with Velorix Esports tournaments, wallet payouts, and account support. How can I help you with your gaming account today?"

            SUPPORT CAPABILITIES:
            1. Tournament Registration, Team Squad Joining & Match Access.
            2. Room ID & Room Password access (credentials released 15 mins prior to match time).
            3. Free Fire UID / BGMI IGN linking & profile verification.
            4. Wallet Deposits, UPI Withdrawals & Instant Prize Payouts.
            5. Match Rules, Fair Play & Anti-Cheat Policies.
            6. Screenshot Verification (UPI receipts, Game profile screenshots, Match results).

            ROW-LEVEL SECURITY & DATA PRIVACY (RLS):
            - You only possess context for the currently authenticated user session.
            - Never invent, attempt to access, or reveal personal data, transaction records, or credentials belonging to other players.
            - When analyzing screenshots, verify transaction IDs, UPI IDs, or In-Game UIDs against the player's active session data.

            ESCALATIONS & UNRECEIVED PAYOUTS (>1-2 HOURS) / BUGS:
            - If the player reports a bug, app error, or a wallet payout/withdrawal that has been delayed over 1 to 2 hours, reassure them politely: "Your query has been logged and flagged for priority review by Velorix Admin Support. Our team will verify your account details and resolve your request promptly."

            Keep all responses clear, professional, structured with clean formatting/emojis, and strictly relevant to Velorix Esports.
        """.trimIndent()

        return if (!userContextSummary.isNullOrBlank()) {
            "$base\n\n[Active Authenticated Player Context (RLS Restricted)]:\n$userContextSummary"
        } else {
            base
        }
    }

    suspend fun getResponse(
        userMessage: String,
        imageUriBase64: String? = null,
        previousMessages: List<Pair<String, Boolean>> = emptyList(),
        customApiKey: String? = null,
        modelName: String = "gemini-3.5-flash",
        useThinking: Boolean = false,
        userContextSummary: String? = null
    ): String = withContext(Dispatchers.IO) {
        val buildConfigKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        val effectiveKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey.trim()
            buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY" && buildConfigKey != "DEFAULT_GEMINI_API_KEY" -> buildConfigKey.trim()
            else -> ""
        }

        if (effectiveKey.isBlank()) {
            val offlineReply = getOfflineSmartResponse(userMessage)
            val keyTip = "\n\n*(Tip: To enable live Gemini AI, tap the Key Settings icon in top right or configure GEMINI_API_KEY in AI Studio Secrets.)*"
            return@withContext offlineReply + keyTip
        }

        try {
            val contentsList = mutableListOf<GeminiContent>()

            // Context history (up to last 6 turns)
            previousMessages.takeLast(6).forEach { (text, isUser) ->
                contentsList.add(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = text)),
                        role = if (isUser) "user" else "model"
                    )
                )
            }

            // User turn with optional image
            val userParts = mutableListOf<GeminiPart>()
            userParts.add(GeminiPart(text = userMessage))
            if (!imageUriBase64.isNullOrBlank()) {
                userParts.add(
                    GeminiPart(
                        inlineData = GeminiInlineData(
                            mimeType = "image/jpeg",
                            data = imageUriBase64
                        )
                    )
                )
            }

            contentsList.add(
                GeminiContent(
                    parts = userParts,
                    role = "user"
                )
            )

            val endpointModel = when (modelName) {
                "gemini-3.1-pro-preview" -> "gemini-3.1-pro-preview"
                "gemini-3.1-flash-lite-preview" -> "gemini-3.1-flash-lite-preview"
                else -> "gemini-3.5-flash"
            }

            val genConfig = if (endpointModel == "gemini-3.1-pro-preview" && useThinking) {
                GeminiGenerationConfig(thinkingConfig = GeminiThinkingConfig(thinkingLevel = "high"))
            } else {
                GeminiGenerationConfig(temperature = 0.7f)
            }

            val requestObject = GeminiRequest(
                contents = contentsList,
                generationConfig = genConfig,
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = buildSystemPrompt(userContextSummary))))
            )

            val jsonBody = json.encodeToString(GeminiRequest.serializer(), requestObject)
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$endpointModel:generateContent?key=$effectiveKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiSupportService", "Gemini HTTP failure ${response.code}: $responseString")
                if (response.code == 429) {
                    return@withContext "**Rate Limit Exceeded (HTTP 429)**\nThe Gemini AI request limit has been momentarily reached. Please wait 10-20 seconds before asking your next question."
                }
                val keyHelp = if (response.code in listOf(400, 401, 403)) {
                    "\n\n*(Gemini API Key status ${response.code}. Please update your key in Key Settings or AI Studio Secrets.)*"
                } else {
                    "\n\n*(Gemini API status ${response.code}.)*"
                }
                return@withContext getOfflineSmartResponse(userMessage) + keyHelp
            }

            if (responseString.startsWith("<") || !responseString.trim().startsWith("{")) {
                Log.e("GeminiSupportService", "Received HTML response instead of JSON: $responseString")
                return@withContext getOfflineSmartResponse(userMessage)
            }

            val parsedJson = json.parseToJsonElement(responseString)
            val candidates = parsedJson.jsonObject["candidates"]?.jsonArray
            val replyText = candidates?.getOrNull(0)?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray
                ?.getOrNull(0)?.jsonObject
                ?.get("text")?.jsonPrimitive?.content

            return@withContext replyText?.trim() ?: getOfflineSmartResponse(userMessage)
        } catch (e: Throwable) {
            Log.e("GeminiSupportService", "Error calling Gemini API", e)
            return@withContext getOfflineSmartResponse(userMessage)
        }
    }

    private fun getOfflineSmartResponse(userMessage: String): String {
        val query = userMessage.lowercase()
        return when {
            "room" in query || "password" in query || "id" in query ->
                "**Room ID & Password Access:**\n• Room credentials are sent **15 minutes before match start time**.\n• Go to **My Matches** or tap on your registered tournament card to reveal the Room ID & Password.\n• Ensure your registered In-Game Name matches your actual game account!"

            "withdraw" in query || "money" in query || "payout" in query || "upi" in query || "paytm" in query ->
                "**Withdrawals & Wallet Support:**\n• Open the **Wallet** tab from the bottom navigation.\n• Tap **Withdraw Funds** and enter your UPI ID or Paytm mobile number.\n• Payouts are verified and processed instantly (5-30 minutes)."

            "free fire" in query || "uid" in query || "game id" in query || "ign" in query ->
                "**Free Fire UID Linking:**\n• Go to **Profile Settings**.\n• Scroll down to **Gaming Identity**.\n• Enter your exact Free Fire UID & In-Game Name, then tap **Save**!"

            "register" in query || "join" in query || "tournament" in query || "play" in query ->
                "**How to Join Tournaments:**\n1. Select any upcoming tournament on the **Home** or **Matches** screen.\n2. Review the match fee, rules, and map details.\n3. Tap **JOIN TOURNAMENT** and confirm your entry!"

            "image" in query || "screenshot" in query || "verify" in query ->
                "**Image & Screenshot Verification:**\nAttach a screenshot of your UPI payment receipt, Free Fire profile, or match results using the attach button. Velorix AI will inspect and verify your details."

            else ->
                "**Velorix AI Support:**\nHello Gamer! I am your Velorix Support Assistant. Ask me about:\n• Room ID & Password details\n• Tournament Rules & Joining\n• Free Fire UID linking\n• Wallet Deposits & Screenshot Verification"
        }
    }
}
