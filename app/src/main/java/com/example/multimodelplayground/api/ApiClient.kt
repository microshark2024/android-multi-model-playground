package com.example.multimodelplayground.api

import com.example.multimodelplayground.data.ModelConfig
import com.example.multimodelplayground.data.ModelType
import com.example.multimodelplayground.data.ProviderType
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClient {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    @Throws(IOException::class)
    fun testConnection(config: ModelConfig, callback: (Boolean, String) -> Unit) {
        val prompt = "ping"
        if (config.modelType == ModelType.TEXT) {
            callTextModel(config, listOf(Message("user", prompt)), null) { success, result ->
                callback(success, if (success) "Connection successful: $result" else "Failed: $result")
            }
        } else if (config.modelType == ModelType.IMAGE) {
            callImageModel(config, "A simple circle") { success, result ->
                callback(success, if (success) "Connection successful (Image URL returned)" else "Failed: $result")
            }
        } else {
            callback(true, "Video endpoint configured. Press generate to test.")
        }
    }

    fun callTextModel(
        config: ModelConfig,
        messages: List<Message>,
        base64Image: String?,
        callback: (Boolean, String) -> Unit
    ) {
        val apiKey = config.apiKey
        val baseUrl = config.baseUrl.trimEnd('/')
        val modelId = config.modelId

        val requestBuilder = Request.Builder()
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val requestBodyJson: String
        val url: String

        if (config.providerType == ProviderType.GEMINI) {
            url = "$baseUrl/v1beta/models/$modelId:generateContent?key=$apiKey"
            
            val contentsList = messages.map { msg ->
                val parts = mutableListOf<Map<String, Any>>()
                if (msg.role == "user" && base64Image != null) {
                    parts.add(mapOf(
                        "inlineData" to mapOf(
                            "mimeType" to "image/jpeg",
                            "data" to base64Image
                        )
                    ))
                }
                parts.add(mapOf("text" to msg.content))
                mapOf("role" to if (msg.role == "assistant") "model" else "user", "parts" to parts)
            }
            requestBodyJson = gson.toJson(mapOf("contents" to contentsList))
        } else {
            url = "$baseUrl/chat/completions"
            requestBuilder.header("Authorization", "Bearer $apiKey")

            val openAiMessages = messages.map { msg ->
                if (msg.role == "user" && base64Image != null) {
                    val contentList = listOf(
                        mapOf("type" to "text", "text" to msg.content),
                        mapOf(
                            "type" to "image_url",
                            "image_url" to mapOf("url" to "data:image/jpeg;base64,$base64Image")
                        )
                    )
                    mapOf("role" to msg.role, "content" to contentList)
                } else {
                    mapOf("role" to msg.role, "content" to msg.content)
                }
            }
            requestBodyJson = gson.toJson(mapOf("model" to modelId, "messages" to openAiMessages))
        }

        val body = requestBodyJson.toRequestBody(mediaType)
        val request = requestBuilder.url(url).post(body).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback(false, e.message ?: "Unknown network failure")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val respBody = response.body?.string()
                    if (!response.isSuccessful) {
                        callback(false, "Error ${response.code}: ${respBody ?: "No details"}")
                        return
                    }
                    if (respBody == null) {
                        callback(false, "Empty response body")
                        return
                    }

                    try {
                        val parsedText = if (config.providerType == ProviderType.GEMINI) {
                            parseGeminiResponse(respBody)
                        } else {
                            parseOpenAiResponse(respBody)
                        }
                        callback(true, parsedText)
                    } catch (e: Exception) {
                        callback(false, "Parse error: ${e.message}\nResponse was: $respBody")
                    }
                }
            }
        })
    }

    fun callImageModel(
        config: ModelConfig,
        prompt: String,
        callback: (Boolean, String) -> Unit
    ) {
        val apiKey = config.apiKey
        val baseUrl = config.baseUrl.trimEnd('/')
        val modelId = config.modelId
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val requestBuilder = Request.Builder()
        val url = "$baseUrl/images/generations"
        requestBuilder.header("Authorization", "Bearer $apiKey")
        
        val requestBodyJson = gson.toJson(mapOf("model" to modelId, "prompt" to prompt, "n" to 1, "size" to "1024x1024"))

        val body = requestBodyJson.toRequestBody(mediaType)
        val request = requestBuilder.url(url).post(body).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback(false, e.message ?: "Unknown network failure")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val respBody = response.body?.string()
                    if (!response.isSuccessful) {
                        callback(false, "Error ${response.code}: ${respBody ?: "No details"}")
                        return
                    }
                    if (respBody == null) {
                        callback(false, "Empty response body")
                        return
                    }

                    try {
                        val urlFound = findUrlInJson(respBody)
                        if (urlFound != null) {
                            callback(true, urlFound)
                        } else {
                            callback(false, "Could not find image URL in response: $respBody")
                        }
                    } catch (e: Exception) {
                        callback(false, "Parse error: ${e.message}")
                    }
                }
            }
        })
    }

    fun callVideoModel(
        config: ModelConfig,
        prompt: String,
        imageUrl: String?,
        callback: (Boolean, String) -> Unit
    ) {
        val apiKey = config.apiKey
        val baseUrl = config.baseUrl.trimEnd('/')
        val modelId = config.modelId
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val requestBuilder = Request.Builder()
        val url = if (config.providerType == ProviderType.CUSTOM && baseUrl.contains("fal.run")) {
            "$baseUrl/$modelId"
        } else {
            baseUrl
        }

        if (apiKey.isNotEmpty()) {
            if (url.contains("fal.run")) {
                requestBuilder.header("Authorization", "Key $apiKey")
            } else {
                requestBuilder.header("Authorization", "Bearer $apiKey")
            }
        }

        val requestBodyMap = mutableMapOf<String, Any>("prompt" to prompt)
        if (imageUrl != null) {
            requestBodyMap["image_url"] = imageUrl
        }
        val requestBodyJson = gson.toJson(requestBodyMap)

        val body = requestBodyJson.toRequestBody(mediaType)
        val request = requestBuilder.url(url).post(body).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback(false, e.message ?: "Failed to submit video request")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val respBody = response.body?.string()
                    if (!response.isSuccessful) {
                        callback(false, "Error ${response.code}: ${respBody ?: "No details"}")
                        return
                    }
                    if (respBody == null) {
                        callback(false, "Empty response body")
                        return
                    }

                    try {
                        val jsonObject = gson.fromJson(respBody, JsonObject::class.java)
                        
                        val directVideoUrl = findUrlInJson(respBody, listOf(".mp4", ".webm"))
                        if (directVideoUrl != null) {
                            callback(true, directVideoUrl)
                            return
                        }

                        val requestId = jsonObject.get("request_id")?.asString
                        if (requestId != null && url.contains("fal.run")) {
                            pollFalQueue(baseUrl, modelId, apiKey, requestId, callback)
                        } else {
                            callback(false, "Couldn't locate direct video URL or Queue ID in: $respBody")
                        }
                    } catch (e: Exception) {
                        callback(false, "Parse error: ${e.message}")
                    }
                }
            }
        })
    }

    private fun pollFalQueue(
        baseUrl: String,
        modelId: String,
        apiKey: String,
        requestId: String,
        callback: (Boolean, String) -> Unit
    ) {
        val statusUrl = "$baseUrl/$modelId/requests/$requestId"
        val request = Request.Builder()
            .url(statusUrl)
            .header("Authorization", "Key $apiKey")
            .get()
            .build()

        fun runPoll(attempts: Int) {
            if (attempts > 30) {
                callback(false, "Video generation timed out.")
                return
            }

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    callback(false, "Polling failure: ${e.message}")
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use {
                        val respBody = response.body?.string()
                        if (!response.isSuccessful || respBody == null) {
                            callback(false, "Polling error ${response.code}")
                            return
                        }

                        try {
                            val jsonObject = gson.fromJson(respBody, JsonObject::class.java)
                            val status = jsonObject.get("status")?.asString
                            
                            if (status == "COMPLETED") {
                                val videoUrl = findUrlInJson(respBody, listOf(".mp4", ".webm"))
                                if (videoUrl != null) {
                                    callback(true, videoUrl)
                                } else {
                                    callback(false, "Video completed but url not found in: $respBody")
                                }
                            } else if (status == "FAILED") {
                                callback(false, "Video generation failed in queue.")
                            } else {
                                Thread.sleep(4000)
                                runPoll(attempts + 1)
                            }
                        } catch (e: Exception) {
                            callback(false, "Polling parse error: ${e.message}")
                        }
                    }
                }
            })
        }

        Thread {
            try {
                runPoll(1)
            } catch (e: Exception) {
                callback(false, "Polling thread error: ${e.message}")
            }
        }.start()
    }

    private fun parseOpenAiResponse(json: String): String {
        val jsonObject = gson.fromJson(json, JsonObject::class.java)
        val choices = jsonObject.getAsJsonArray("choices")
        val message = choices.get(0).asJsonObject.getAsJsonObject("message")
        return message.get("content").asString
    }

    private fun parseGeminiResponse(json: String): String {
        val jsonObject = gson.fromJson(json, JsonObject::class.java)
        val candidates = jsonObject.getAsJsonArray("candidates")
        val firstCandidate = candidates.get(0).asJsonObject
        val content = firstCandidate.getAsJsonObject("content")
        val parts = content.getAsJsonArray("parts")
        return parts.get(0).asJsonObject.get("text").asString
    }

    private fun findUrlInJson(json: String, extensions: List<String> = listOf("http", "https")): String? {
        val root = gson.fromJson(json, JsonElement::class.java) ?: return null
        return searchJsonForUrl(root, extensions)
    }

    private fun searchJsonForUrl(element: JsonElement, extensions: List<String>): String? {
        if (element.isJsonPrimitive) {
            val str = element.asString
            val matchesExtension = extensions.any { ext -> str.contains(ext, ignoreCase = true) }
            if (matchesExtension && (str.startsWith("http://") || str.startsWith("https://") || str.startsWith("data:image"))) {
                return str
            }
        } else if (element.isJsonObject) {
            val obj = element.asJsonObject
            for (entry in obj.entrySet()) {
                val found = searchJsonForUrl(entry.value, extensions)
                if (found != null) return found
            }
        } else if (element.isJsonArray) {
            val arr = element.asJsonArray
            for (item in arr) {
                val found = searchJsonForUrl(item, extensions)
                if (found != null) return found
            }
        }
        return null
    }
}

data class Message(
    val role: String,
    val content: String
)
