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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.multimodelplayground.api.ApiClient
import com.example.multimodelplayground.data.ConfigStorage
import com.example.multimodelplayground.data.ModelConfig
import com.example.multimodelplayground.data.ModelType
import com.example.multimodelplayground.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(
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
    var generatedVideoUrl by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    val suggestions = listOf(
        "一条巨龙在雪山顶飞过，慢动作，史诗级画质",
        "无人机航拍秋天的金色森林，有一条小溪蜿蜒穿过",
        "雨中的赛博朋克都市，霓虹倒影，电影质感",
        "一朵花在微距镜头下缓慢绽放的过程",
        "可爱的仓鼠戴着厨师帽在微型厨房做饭"
    )

    LaunchedEffect(Unit) {
        activeConfig = storage.getActiveConfig(ModelType.VIDEO)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AI 视频生成",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        activeConfig?.let {
                            Text(
                                "当前模型: ${it.name}",
                                color = PinkGlow,
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
                        Icon(Icons.Default.Settings, contentDescription = "配置模型", tint = PinkGlow)
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
                            "未配置可用的视频生成模型",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "视频生成需要配置专用的视频生成接口。例如 Fal.ai Luma/Kling 接口。点击下方按钮进行配置。",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Button(
                            onClick = onNavigateToConfig,
                            colors = ButtonDefaults.buttonColors(containerColor = PinkGlow, contentColor = Color.White)
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
                // Video Screen Canvas Player
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.778f) // 16:9 Aspect Ratio
                        .border(1.dp, SlateCard, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (generatedVideoUrl != null) {
                            VideoPlayer(
                                videoUrl = generatedVideoUrl!!,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (isGenerating) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = PinkGlow)
                                Text("视频正在队列生成中...", color = TextSecondary, fontSize = 13.sp)
                                Text("这通常需要 30秒 - 1分钟，请耐心等待", color = TextMuted, fontSize = 11.sp)
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "视频",
                                    tint = TextMuted,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text("播放器已就绪", color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text("在下方输入您的创意场景，开始生成电影镜头", color = TextMuted, fontSize = 12.sp)
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

                // Suggestions Panel
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("推荐场景:", color = TextSecondary, fontSize = 12.sp)
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
                        focusedBorderColor = PinkGlow,
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
                    if (generatedVideoUrl != null) {
                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, generatedVideoUrl)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "分享视频链接"))
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
                                generatedVideoUrl = null

                                coroutineScope.launch(Dispatchers.IO) {
                                    apiClient.callVideoModel(
                                        config = activeConfig!!,
                                        prompt = prompt.trim(),
                                        imageUrl = null
                                    ) { success, result ->
                                        coroutineScope.launch(Dispatchers.Main) {
                                            isGenerating = false
                                            if (success) {
                                                generatedVideoUrl = result
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
                            containerColor = PinkGlow,
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
                            Icon(Icons.Default.PlayArrow, contentDescription = "开始生成", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("生成视频", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                setBackgroundColor(0xFF000000.toInt())
            }
        },
        modifier = modifier
    )
}
