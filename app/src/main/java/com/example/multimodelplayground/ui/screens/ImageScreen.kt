package com.example.multimodelplayground.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.multimodelplayground.api.ApiClient
import com.example.multimodelplayground.data.ConfigStorage
import com.example.multimodelplayground.data.ModelConfig
import com.example.multimodelplayground.data.ModelType
import com.example.multimodelplayground.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageScreen(
    onNavigateToConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storage = remember { ConfigStorage(context) }
    val apiClient = remember { ApiClient() }
    val coroutineScope = rememberCoroutineScope()

    var activeConfig by remember { mutableStateOf<ModelConfig?>(null) }
    var prompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedImageUrl by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    val suggestions = listOf(
        "塞伯朋克风格的繁华街道，霓虹灯闪烁",
        "水彩画风的宁静中式江南小镇",
        "宇航员在月球表面弹吉他，写实风格",
        "复古未来主义的悬浮飞车",
        "一只穿着西装打领带的可爱小猫咪"
    )

    LaunchedEffect(Unit) {
        activeConfig = storage.getActiveConfig(ModelType.IMAGE)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AI 图像生成",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        activeConfig?.let {
                            Text(
                                "当前模型: ${it.name}",
                                color = VioletGlow,
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
                    IconButton(onClick = onNavigateToConfig) {
                        Icon(Icons.Default.Settings, contentDescription = "配置模型", tint = VioletGlow)
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
                            "未配置可用的图像生成模型",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "图像生成功能需要首先配置 API 密钥和接口。例如 OpenAI DALL-E 或 Fal.ai 图像端点。点击下方按钮前往配置。",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Button(
                            onClick = onNavigateToConfig,
                            colors = ButtonDefaults.buttonColors(containerColor = VioletGlow, contentColor = Color.White)
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
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Image Canvas Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .border(1.dp, SlateCard, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (generatedImageUrl != null) {
                            SubcomposeAsyncImage(
                                model = generatedImageUrl,
                                contentDescription = "生成的图片",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = VioletGlow)
                                    }
                                },
                                error = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = "加载出错", tint = ErrorRed)
                                        Text("图片加载失败", color = ErrorRed, fontSize = 12.sp)
                                    }
                                }
                            )
                        } else if (isGenerating) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = VioletGlow)
                                Text("正在生成梦幻画作...", color = TextSecondary, fontSize = 13.sp)
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Create,
                                    contentDescription = "调色板",
                                    tint = TextMuted,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text("画布已就绪", color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text("在下方输入您的创意，开始创作吧", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Error Message
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { err ->
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

                // Suggestions Horizontal Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("激发灵感:", color = TextSecondary, fontSize = 12.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(suggestions) { sug ->
                            Box(
                                modifier = Modifier
                                    .background(SlateCard, RoundedCornerShape(16.dp))
                                    .clickable { prompt = sug }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(sug, color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Input Prompt Field
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("输入创意描述 (Prompt)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = VioletGlow,
                        unfocusedBorderColor = SlateCard,
                        focusedContainerColor = SlateSurface,
                        unfocusedContainerColor = SlateSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Actions Panel
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (generatedImageUrl != null) {
                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, generatedImageUrl)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "分享图片链接"))
                            },
                            border = BorderStroke(1.dp, SlateCard),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "分享", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("分享链接")
                        }
                    }

                    Button(
                        onClick = {
                            if (prompt.trim().isNotEmpty() && !isGenerating && activeConfig != null) {
                                isGenerating = true
                                errorMessage = null
                                generatedImageUrl = null

                                coroutineScope.launch(Dispatchers.IO) {
                                    apiClient.callImageModel(
                                        config = activeConfig!!,
                                        prompt = prompt.trim()
                                    ) { success, result ->
                                        coroutineScope.launch(Dispatchers.Main) {
                                            isGenerating = false
                                            if (success) {
                                                generatedImageUrl = result
                                            } else {
                                                errorMessage = "生成失败: $result"
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        enabled = prompt.trim().isNotEmpty() && !isGenerating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VioletGlow,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Star, contentDescription = "开始生成", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("生成图片", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
