package com.example.multimodelplayground.data

import java.util.UUID

enum class ProviderType {
    OPENAI_COMPATIBLE,
    GEMINI,
    CUSTOM
}

enum class ModelType {
    TEXT,   // Chat & Multimodal
    IMAGE,  // Text-to-Image
    VIDEO   // Text-to-Video
}

data class ModelConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val providerType: ProviderType,
    val modelType: ModelType,
    val baseUrl: String,
    val apiKey: String,
    val modelId: String,
    val isDefault: Boolean = false
)
