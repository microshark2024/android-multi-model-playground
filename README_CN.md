# Android 多模型探索器 (Android Multi-model Playground)

[English](README.md) | [简体中文]

一个基于 Jetpack Compose 构建的 Android 应用程序，用于测试、玩耍和体验各种 AI 模型。它支持跨不同 AI 提供商的文本/对话、图像生成和视频生成。

## 功能特性

- 💬 **对话与多模态界面 (Chat & Multimodal Screen)**：与大语言模型（LLM）进行无缝对话。支持文本对话、历史记录，并集成了多模态输入。
- 🎨 **图像生成 (Text-to-Image)**：根据文本描述动态生成图像。
- 🎬 **视频播放与生成**：使用 Android 的 ExoPlayer/Media3 体验视频生成或查看视频输出。
- ⚙️ **灵活的模型配置 (Model Configuration)**：
  - 支持多个 AI 提供商（Gemini、OpenAI 兼容接口、自定义端点）。
  - 定义自定义模型类型（文本、图像、视频）。
  - 安全地本地存储 API 密钥 and Base URL。

## 技术栈

- **UI 框架**：Jetpack Compose
- **编程语言**：Kotlin + 协程 (Coroutines)
- **网络请求**：Retrofit & OkHttp
- **JSON 序列化**：Kotlinx Serialization / Gson
- **图片加载**：Coil
- **视频播放**：Media3 (ExoPlayer)
- **导航**：Jetpack Navigation3
- **依赖注入/清洁架构**：采用 Repository 模式进行本地配置存储和 API 交互。

## 运行要求

- **最低 Android SDK**：API 24 (Android 8.0)
- **目标 Android SDK**：API 36
- **JDK 版本**：Java 17

## 快速上手

1. **安装 JDK 17**：确保您的电脑上已安装 Java 17。
2. **构建项目**：运行 `./gradlew assembleDebug` 以构建 debug 版本，或直接下载预编译的 [Release APK](https://github.com/microshark2024/android-multi-model-playground/releases/tag/v1.0.0)。
3. **配置 API 密钥**：
   - 打开 App。
   - 前往 **Config** (设置) 页面。
   - 添加您的 Gemini API 密钥或自定义的 OpenAI 兼容端点信息。
   - 开始对话或生成媒体！
