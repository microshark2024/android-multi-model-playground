package com.example.multimodelplayground.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.multimodelplayground.*
import com.example.multimodelplayground.theme.*

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBg)
      .padding(16.dp)
      .verticalScroll(scrollState)
      .safeDrawingPadding(),
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    // Top Hero Banner
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          brush = Brush.linearGradient(listOf(CyanGlow.copy(alpha = 0.08f), VioletGlow.copy(alpha = 0.08f))),
          shape = RoundedCornerShape(20.dp)
        )
        .border(
          width = 1.dp,
          brush = Brush.linearGradient(listOf(CyanGlow.copy(alpha = 0.2f), VioletGlow.copy(alpha = 0.2f))),
          shape = RoundedCornerShape(20.dp)
        )
        .padding(24.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(
            Icons.Default.Star,
            contentDescription = "星星",
            tint = CyanGlow,
            modifier = Modifier.size(24.dp)
          )
          Text(
            "多模型智脑空间",
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            fontSize = 22.sp
          )
        }
        Text(
          "在一个应用中调度市面最顶尖的语言、视觉及视频生成模型。配置专属的密钥与接口，即刻开始交互探索。",
          color = TextSecondary,
          fontSize = 13.sp,
          lineHeight = 18.sp
        )
      }
    }

    Text(
      "模型应用能力",
      fontWeight = FontWeight.Bold,
      color = Color.White,
      fontSize = 16.sp
    )

    // Module Card 1: Chat
    DashboardCard(
      title = "AI 智能聊天 / 多模态",
      description = "支持多轮文本会话，支持上传图片进行视觉内容理解及多模态解析。",
      icon = Icons.Default.Email,
      accentColor = CyanGlow,
      onClick = { onItemClick(Chat) }
    )

    // Module Card 2: Image
    DashboardCard(
      title = "AI 艺术绘画",
      description = "输入创意文字，调用 DALL-E 3、Flux 等端点，秒级生成极具表现力的图像。",
      icon = Icons.Default.Create,
      accentColor = VioletGlow,
      onClick = { onItemClick(ImageGen) }
    )

    // Module Card 3: Video
    DashboardCard(
      title = "AI 视频生成",
      description = "调用 Luma Dream Machine、Kling 等接口，自动排队轮询并原生播放生成的 MP4 视频。",
      icon = Icons.Default.PlayArrow,
      accentColor = PinkGlow,
      onClick = { onItemClick(VideoGen) }
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Settings Quick Launch
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(SlateSurface, RoundedCornerShape(12.dp))
        .border(1.dp, SlateCard, RoundedCornerShape(12.dp))
        .clickable { onItemClick(Config) }
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Icon(
          Icons.Default.Settings,
          contentDescription = "设置",
          tint = CyanGlow,
          modifier = Modifier.size(20.dp)
        )
        Column {
          Text("后台接口与 Key 配置", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Text("配置各种模型的自定义 API 域名及 Key", color = TextMuted, fontSize = 11.sp)
        }
      }
      Icon(
        Icons.Default.ArrowForward,
        contentDescription = "前往",
        tint = TextSecondary
      )
    }
  }
}

@Composable
fun DashboardCard(
  title: String,
  description: String,
  icon: ImageVector,
  accentColor: Color,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, SlateCard, RoundedCornerShape(16.dp))
      .clip(RoundedCornerShape(16.dp))
      .clickable { onClick() },
    colors = CardDefaults.cardColors(containerColor = SlateSurface)
  ) {
    Row(
      modifier = Modifier.padding(20.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
          )
          Text(
            title,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 16.sp
          )
        }
        Text(
          description,
          color = TextSecondary,
          fontSize = 12.sp,
          lineHeight = 16.sp
        )
      }
      Icon(
        Icons.Default.ArrowForward,
        contentDescription = "进入",
        tint = accentColor.copy(alpha = 0.8f),
        modifier = Modifier
          .padding(start = 12.dp)
          .size(16.dp)
      )
    }
  }
}
