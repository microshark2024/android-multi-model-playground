package com.example.multimodelplayground.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ConfigStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("model_playground_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveConfigs(configs: List<ModelConfig>) {
        val json = gson.toJson(configs)
        prefs.edit().putString("model_configs", json).apply()
    }

    fun getConfigs(): List<ModelConfig> {
        val json = prefs.getString("model_configs", null) ?: return getPredefinedConfigs()
        val type = object : TypeToken<List<ModelConfig>>() {}.type
        return gson.fromJson(json, type) ?: getPredefinedConfigs()
    }

    fun getActiveConfig(type: ModelType): ModelConfig? {
        val configs = getConfigs().filter { it.modelType == type }
        return configs.firstOrNull { it.isDefault } ?: configs.firstOrNull()
    }

    fun selectConfig(id: String, type: ModelType) {
        val configs = getConfigs().map {
            if (it.modelType == type) {
                it.copy(isDefault = it.id == id)
            } else {
                it
            }
        }
        saveConfigs(configs)
    }

    private fun getPredefinedConfigs(): List<ModelConfig> {
        return listOf(
            ModelConfig(
                name = "DeepSeek Chat (Default)",
                providerType = ProviderType.OPENAI_COMPATIBLE,
                modelType = ModelType.TEXT,
                baseUrl = "https://api.deepseek.com/v1",
                apiKey = "",
                modelId = "deepseek-chat",
                isDefault = true
            ),
            ModelConfig(
                name = "Gemini Flash (Default)",
                providerType = ProviderType.GEMINI,
                modelType = ModelType.TEXT,
                baseUrl = "https://generativelanguage.googleapis.com",
                apiKey = "",
                modelId = "gemini-1.5-flash",
                isDefault = false
            ),
            ModelConfig(
                name = "DALL-E 3 (Default)",
                providerType = ProviderType.OPENAI_COMPATIBLE,
                modelType = ModelType.IMAGE,
                baseUrl = "https://api.openai.com/v1",
                apiKey = "",
                modelId = "dall-e-3",
                isDefault = true
            ),
            ModelConfig(
                name = "Fal.ai Luma (Default Video)",
                providerType = ProviderType.CUSTOM,
                modelType = ModelType.VIDEO,
                baseUrl = "https://queue.fal.run/fal-ai",
                apiKey = "",
                modelId = "luma-dream-machine",
                isDefault = true
            )
        )
    }
}
