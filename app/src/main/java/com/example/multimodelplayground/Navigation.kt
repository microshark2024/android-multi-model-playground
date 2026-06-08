package com.example.multimodelplayground

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.multimodelplayground.ui.main.MainScreen
import com.example.multimodelplayground.ui.screens.ChatScreen
import com.example.multimodelplayground.ui.screens.ConfigScreen
import com.example.multimodelplayground.ui.screens.ImageScreen
import com.example.multimodelplayground.ui.screens.VideoScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
            onItemClick = { navKey -> backStack.add(navKey) },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<Chat> {
          ChatScreen(
            onNavigateToConfig = { backStack.add(Config) },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<ImageGen> {
          ImageScreen(
            onNavigateToConfig = { backStack.add(Config) },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<VideoGen> {
          VideoScreen(
            onNavigateToConfig = { backStack.add(Config) },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<Config> {
          ConfigScreen(
            onBackClick = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
}
