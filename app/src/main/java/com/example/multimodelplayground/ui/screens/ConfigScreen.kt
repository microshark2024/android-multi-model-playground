package com.example.multimodelplayground.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.multimodelplayground.api.ApiClient
import com.example.multimodelplayground.data.ConfigStorage
import com.example.multimodelplayground.data.ModelConfig
import com.example.multimodelplayground.data.ModelType
import com.example.multimodelplayground.data.ProviderType
import com.example.multimodelplayground.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storage = remember { ConfigStorage(context) }
    val apiClient = remember { ApiClient() }
    val coroutineScope = rememberCoroutineScope()

    var configs by remember { mutableStateOf(storage.getConfigs()) }
    var selectedTab by remember { mutableStateOf(ModelType.TEXT) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<ModelConfig?>(null) }

    // Refresh configurations list
    val refreshConfigs = {
        configs = storage.getConfigs()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "后台模型配置",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBg
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingConfig = null
                    showAddDialog = true
                },
                containerColor = CyanGlow,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加配置")
            }
        },
        containerColor = ObsidianBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Category Tabs
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = ObsidianBg,
                contentColor = CyanGlow,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = CyanGlow
                    )
                }
            ) {
                ModelType.values().forEach { type ->
                    Tab(
                        selected = selectedTab == type,
                        onClick = { selectedTab = type },
                        text = {
                            Text(
                                when (type) {
                                    ModelType.TEXT -> "文本/多模态"
                                    ModelType.IMAGE -> "图片生成"
                                    ModelType.VIDEO -> "视频生成"
                                },
                                color = if (selectedTab == type) CyanGlow else TextSecondary,
                                fontWeight = if (selectedTab == type) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val filteredConfigs = configs.filter { it.modelType == selectedTab }

            if (filteredConfigs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无配置，请点击右下角添加", color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredConfigs) { config ->
                        ConfigCard(
                            config = config,
                            onEdit = {
                                editingConfig = config
                                showAddDialog = true
                            },
                            onDelete = {
                                val updated = configs.filter { it.id != config.id }
                                storage.saveConfigs(updated)
                                refreshConfigs()
                            },
                            onSelectActive = {
                                storage.selectConfig(config.id, config.modelType)
                                refreshConfigs()
                            },
                            apiClient = apiClient,
                            coroutineScope = coroutineScope
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditConfigDialog(
            config = editingConfig,
            defaultModelType = selectedTab,
            onDismiss = { showAddDialog = false },
            onSave = { newConfig ->
                val currentList = configs.toMutableList()
                val index = currentList.indexOfFirst { it.id == newConfig.id }
                if (index != -1) {
                    currentList[index] = newConfig
                } else {
                    // If first of this type, make it default
                    val hasDefault = currentList.any { it.modelType == newConfig.modelType && it.isDefault }
                    currentList.add(newConfig.copy(isDefault = !hasDefault))
                }
                storage.saveConfigs(currentList)
                refreshConfigs()
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ConfigCard(
    config: ModelConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSelectActive: () -> Unit,
    apiClient: ApiClient,
    coroutineScope: CoroutineScope
) {
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val borderBrush = Brush.linearGradient(
        colors = if (config.isDefault) listOf(CyanGlow, VioletGlow) else listOf(SlateCard, SlateCard)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderBrush, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        config.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "模型: ${config.modelId} (${config.providerType})",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = config.isDefault,
                        onClick = onSelectActive,
                        colors = RadioButtonDefaults.colors(selectedColor = CyanGlow)
                    )
                    Text(
                        if (config.isDefault) "已启用" else "启用",
                        color = if (config.isDefault) CyanGlow else TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onSelectActive() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = SlateCard, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Text("接口地址 (Base URL):", color = TextMuted, fontSize = 11.sp)
            Text(config.baseUrl, color = TextSecondary, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = TextSecondary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = ErrorRed)
                }
                Button(
                    onClick = {
                        testing = true
                        testResult = null
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                apiClient.testConnection(config) { success, msg ->
                                    testResult = Pair(success, msg)
                                    testing = false
                                }
                            } catch (e: Exception) {
                                testResult = Pair(false, e.message ?: "连接失败")
                                testing = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (testResult?.first == true) SuccessGreen else SlateCard,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    if (testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (testResult?.first == true) "测试成功" else "测试连接",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            AnimatedVisibility(visible = testResult != null) {
                testResult?.let { result ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(
                                if (result.first) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f),
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (result.first) SuccessGreen.copy(alpha = 0.4f) else ErrorRed.copy(alpha = 0.4f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            result.second,
                            color = if (result.first) SuccessGreen else ErrorRed,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditConfigDialog(
    config: ModelConfig?,
    defaultModelType: ModelType,
    onDismiss: () -> Unit,
    onSave: (ModelConfig) -> Unit
) {
    var name by remember { mutableStateOf(config?.name ?: "") }
    var providerType by remember { mutableStateOf(config?.providerType ?: ProviderType.OPENAI_COMPATIBLE) }
    var modelType by remember { mutableStateOf(config?.modelType ?: defaultModelType) }
    var baseUrl by remember { mutableStateOf(config?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(config?.apiKey ?: "") }
    var modelId by remember { mutableStateOf(config?.modelId ?: "") }
    var apiKeyVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateCard, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (config == null) "添加模型配置" else "编辑模型配置",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("配置名称 (如: DeepSeek-Text)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyanGlow,
                        unfocusedBorderColor = SlateCard
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Provider Type Dropdown / Row
                Column {
                    Text("服务商类型:", color = TextSecondary, fontSize = 12.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProviderType.values().forEach { provider ->
                            val selected = providerType == provider
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (selected) CyanGlow.copy(alpha = 0.15f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) CyanGlow else SlateCard,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        providerType = provider
                                        // Auto fill default URLs for common providers
                                        if (baseUrl.isEmpty() || baseUrl == "https://api.openai.com/v1" || baseUrl == "https://generativelanguage.googleapis.com" || baseUrl == "https://queue.fal.run/fal-ai") {
                                            baseUrl = when (provider) {
                                                ProviderType.OPENAI_COMPATIBLE -> "https://api.openai.com/v1"
                                                ProviderType.GEMINI -> "https://generativelanguage.googleapis.com"
                                                ProviderType.CUSTOM -> "https://queue.fal.run/fal-ai"
                                            }
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    when (provider) {
                                        ProviderType.OPENAI_COMPATIBLE -> "OpenAI格式"
                                        ProviderType.GEMINI -> "Gemini原生"
                                        ProviderType.CUSTOM -> "自定义/视频"
                                    },
                                    color = if (selected) CyanGlow else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Model Type Select
                Column {
                    Text("模型用途:", color = TextSecondary, fontSize = 12.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ModelType.values().forEach { type ->
                            val selected = modelType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (selected) VioletGlow.copy(alpha = 0.15f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) VioletGlow else SlateCard,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { modelType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    when (type) {
                                        ModelType.TEXT -> "文本/多模态"
                                        ModelType.IMAGE -> "图像生成"
                                        ModelType.VIDEO -> "视频生成"
                                    },
                                    color = if (selected) VioletGlow else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Base URL
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("接口地址 (Base URL)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyanGlow,
                        unfocusedBorderColor = SlateCard
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // API Key
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key / 密钥") },
                    singleLine = true,
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(
                                imageVector = if (apiKeyVisible) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = if (apiKeyVisible) "隐藏密码" else "显示密码",
                                tint = TextSecondary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyanGlow,
                        unfocusedBorderColor = SlateCard
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Model ID
                OutlinedTextField(
                    value = modelId,
                    onValueChange = { modelId = it },
                    label = { Text("模型标识 (Model ID, 如: gpt-4o)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyanGlow,
                        unfocusedBorderColor = SlateCard
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, SlateCard),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }

                    Button(
                        onClick = {
                            if (name.isNotEmpty() && baseUrl.isNotEmpty() && modelId.isNotEmpty()) {
                                onSave(
                                    ModelConfig(
                                        id = config?.id ?: java.util.UUID.randomUUID().toString(),
                                        name = name,
                                        providerType = providerType,
                                        modelType = modelType,
                                        baseUrl = baseUrl,
                                        apiKey = apiKey,
                                        modelId = modelId,
                                        isDefault = config?.isDefault ?: false
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanGlow,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotEmpty() && baseUrl.isNotEmpty() && modelId.isNotEmpty()
                    ) {
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
