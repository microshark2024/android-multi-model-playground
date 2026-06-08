package com.example.multimodelplayground.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.multimodelplayground.api.ApiClient
import com.example.multimodelplayground.api.Message
import com.example.multimodelplayground.data.ConfigStorage
import com.example.multimodelplayground.data.ModelConfig
import com.example.multimodelplayground.data.ModelType
import com.example.multimodelplayground.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream

data class ChatMessage(
    val role: String,
    val text: String,
    val imageUri: Uri? = null,
    val base64Image: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val storage = remember { ConfigStorage(context) }
    val apiClient = remember { ApiClient() }
    val coroutineScope = rememberCoroutineScope()

    var activeConfig by remember { mutableStateOf<ModelConfig?>(null) }
    var chatHistory by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    var attachedImageBase64 by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // Reload active config on composition or restart
    LaunchedEffect(Unit) {
        activeConfig = storage.getActiveConfig(ModelType.TEXT)
    }

    // Scroll to bottom when new messages are added
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageUri = uri
            attachedImageBase64 = uriToBase64(context, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AI 聊天 & 多模态",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        activeConfig?.let {
                            Text(
                                "当前模型: ${it.name}",
                                color = CyanGlow,
                                fontSize = 11.sp
                            )
                        } ?: Text(
                            "未选择模型",
                            color = ErrorRed,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        chatHistory = emptyList()
                        errorMessage = null
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "清空对话", tint = TextSecondary)
                    }
                    IconButton(onClick = onNavigateToConfig) {
                        Icon(Icons.Default.Settings, contentDescription = "配置模型", tint = CyanGlow)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg)
            )
        },
        containerColor = ObsidianBg
    ) { paddingValues ->
        if (activeConfig == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    modifier = Modifier.border(1.dp, SlateCard, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "未配置",
                            tint = WarningYellow,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "未配置可用的文本模型",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "调用各种接口前，需要首先配置可用的模型和 API Key。点击下方按钮前往配置。",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Button(
                            onClick = onNavigateToConfig,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanGlow, contentColor = Color.Black)
                        ) {
                            Text("前往后台配置")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (chatHistory.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                               ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "开始对话",
                                        tint = TextMuted,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text("开启一段与 ${activeConfig?.modelId} 的对话吧", color = TextMuted, fontSize = 13.sp)
                                    Text("支持发送文本或上传图片进行解析", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    items(chatHistory) { message ->
                        MessageRow(
                            message = message,
                            onCopyClick = { text ->
                                clipboardManager.setText(AnnotatedString(text))
                            }
                        )
                    }

                    if (isGenerating) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(SlateSurface, RoundedCornerShape(12.dp))
                                        .border(1.dp, SlateCard, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = CyanGlow,
                                            strokeWidth = 2.dp
                                        )
                                        Text("AI 正在思考中...", color = TextSecondary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    errorMessage?.let { err ->
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ErrorRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .border(1.dp, ErrorRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(err, color = ErrorRed, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Image Attachment Preview Banner
                AnimatedVisibility(visible = attachedImageUri != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateSurface)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, CyanGlow, RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = attachedImageUri),
                                contentDescription = "上传图片",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("已附加图片", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("将连同文本一同发送给多模态模型", color = TextSecondary, fontSize = 11.sp)
                        }

                        IconButton(onClick = {
                            attachedImageUri = null
                            attachedImageBase64 = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "移除图片", tint = ErrorRed)
                        }
                    }
                }

                // Input Box Panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateSurface)
                        .padding(12.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { pickImageLauncher.launch("image/*") },
                        modifier = Modifier
                            .background(SlateCard, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "附加图片", tint = CyanGlow)
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("输入消息...", color = TextMuted) },
                        maxLines = 4,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SlateCard,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = SlateCard,
                            unfocusedContainerColor = SlateCard
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )

                    val canSend = inputText.trim().isNotEmpty() || attachedImageBase64 != null
                    IconButton(
                        onClick = {
                            if (canSend && !isGenerating && activeConfig != null) {
                                val userMsgText = inputText.trim()
                                val tempImageUri = attachedImageUri
                                val tempImageBase64 = attachedImageBase64

                                // Add user message to history
                                val userMessage = ChatMessage("user", userMsgText, tempImageUri, tempImageBase64)
                                chatHistory = chatHistory + userMessage

                                // Reset input fields
                                inputText = ""
                                attachedImageUri = null
                                attachedImageBase64 = null
                                errorMessage = null
                                isGenerating = true

                                // Build request payload (convert history to ApiMessages)
                                val historyPayload = chatHistory.map {
                                    Message(it.role, it.text)
                                }

                                coroutineScope.launch(Dispatchers.IO) {
                                    apiClient.callTextModel(
                                        config = activeConfig!!,
                                        messages = historyPayload,
                                        base64Image = tempImageBase64
                                    ) { success, result ->
                                        coroutineScope.launch(Dispatchers.Main) {
                                            isGenerating = false
                                            if (success) {
                                                chatHistory = chatHistory + ChatMessage("assistant", result)
                                            } else {
                                                errorMessage = "生成失败: $result"
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        enabled = canSend && !isGenerating,
                        modifier = Modifier
                            .background(if (canSend) CyanGlow else SlateCard, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "发送",
                            tint = if (canSend) Color.Black else TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageRow(
    message: ChatMessage,
    onCopyClick: (String) -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        Brush.linearGradient(listOf(CyanGlow, VioletGlow)),
                        CircleShape
                    )
                    .padding(1.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SlateSurface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Face,
                        contentDescription = "AI",
                        tint = CyanGlow,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Render User Attached Image
            message.imageUri?.let { uri ->
                Box(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .size(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, SlateCard, RoundedCornerShape(8.dp))
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = uri),
                        contentDescription = "上传的图片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Message Bubble
            Box(
                modifier = Modifier
                    .background(
                        color = if (isUser) SlateCard else SlateSurface,
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isUser) Color.Transparent else SlateCard,
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        )
                    )
                    .padding(12.dp)
            ) {
                Column {
                    SelectionContainer {
                        MarkdownText(text = message.text, onCopyClick = { onCopyClick(message.text) })
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(SlateCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "User",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Simple Composable Markdown / Code block Parser
@Composable
fun MarkdownText(
    text: String,
    onCopyClick: () -> Unit
) {
    val blocks = remember(text) { parseMarkdown(text) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockLayout(code = block.code, language = block.language, onCopy = { onCopyClick() })
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = block.content,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CodeBlockLayout(
    code: String,
    language: String,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateCard, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = ObsidianBg)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateSurface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifEmpty { "code" },
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "复制",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    color = CyanGlow,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

sealed class MarkdownBlock {
    data class Paragraph(val content: String) : MarkdownBlock()
    data class CodeBlock(val code: String, val language: String) : MarkdownBlock()
}

fun parseMarkdown(text: String): List<MarkdownBlock> {
    if (text.isEmpty()) return listOf(MarkdownBlock.Paragraph(""))

    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.split("\n")
    var inCodeBlock = false
    var codeLanguage = ""
    val codeContent = StringBuilder()

    for (line in lines) {
        if (line.trim().startsWith("```")) {
            if (inCodeBlock) {
                // Ending block
                blocks.add(MarkdownBlock.CodeBlock(codeContent.toString().trimEnd(), codeLanguage))
                codeContent.clear()
                inCodeBlock = false
            } else {
                // Starting block
                codeLanguage = line.trim().substring(3).trim()
                inCodeBlock = true
            }
        } else {
            if (inCodeBlock) {
                codeContent.append(line).append("\n")
            } else {
                // Simple parsing for bullet points or lists
                val processedLine = if (line.trim().startsWith("- ")) {
                    "• " + line.trim().substring(2)
                } else if (line.trim().startsWith("* ")) {
                    "• " + line.trim().substring(2)
                } else {
                    line
                }

                // Check if we can merge paragraphs
                if (blocks.isNotEmpty() && blocks.last() is MarkdownBlock.Paragraph) {
                    val lastP = blocks.removeAt(blocks.size - 1) as MarkdownBlock.Paragraph
                    blocks.add(MarkdownBlock.Paragraph(lastP.content + "\n" + processedLine))
                } else {
                    blocks.add(MarkdownBlock.Paragraph(processedLine))
                }
            }
        }
    }

    if (inCodeBlock) {
        // Fallback for unclosed code block
        blocks.add(MarkdownBlock.CodeBlock(codeContent.toString().trimEnd(), codeLanguage))
    }

    return blocks
}

// Utility to convert Uri to base64
fun uriToBase64(context: android.content.Context, uri: Uri): String? {
    var inputStream: InputStream? = null
    return try {
        inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        if (bytes != null) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } else null
    } catch (e: Exception) {
        null
    } finally {
        try {
            inputStream?.close()
        } catch (_: Exception) {}
    }
}
