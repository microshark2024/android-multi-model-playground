# Android Multi-model Playground

An Android application built with Jetpack Compose designed for testing, playing, and interacting with various AI models. It provides support for both text/chat, image generation, and video generation across different AI providers.

## Features

- 💬 **Chat & Multimodal Screen**: Seamlessly converse with LLMs. Supports text conversation, history, and integration with multimodal payloads.
- 🎨 **Image Generation (Text-to-Image)**: Generate images dynamically from text descriptions.
- 🎬 **Video Playback & Generation**: Experience video generation or view video outputs using Android's ExoPlayer/Media3.
- ⚙️ **Flexible Model Configuration**:
  - Support for multiple AI providers (Gemini, OpenAI-compatible, Custom endpoints).
  - Define custom model types (Text, Image, Video).
  - Secure local storage for API keys and base URLs.

## Tech Stack

- **UI Framework**: Jetpack Compose
- **Programming Language**: Kotlin with Coroutines
- **Networking**: Retrofit & OkHttp
- **JSON Serialization**: Kotlinx Serialization / Gson
- **Image Loading**: Coil
- **Video Playback**: Media3 (ExoPlayer)
- **Navigation**: Jetpack Navigation3
- **Dependency Injection / Clean Architecture**: Repository pattern for local config storage and API interactions.

## Requirements

- **Minimum Android SDK**: API 24 (Android 8.0)
- **Target Android SDK**: API 36
- **JDK Version**: Java 17

## Getting Started

1. **Install JDK 17**: Ensure Java 17 is installed on your machine.
2. **Build the Project**: Run `./gradlew assembleDebug` to build the debug version, or use the precompiled release APK.
3. **Configure API Keys**:
   - Open the app.
   - Go to the **Config** (Settings) tab.
   - Add your Gemini API key or custom OpenAI-compatible endpoint details.
   - Start chatting or generating media!
