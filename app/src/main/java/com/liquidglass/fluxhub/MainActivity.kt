package com.liquidglass.fluxhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.liquidglass.fluxhub.chat.ChatScreen
import com.liquidglass.fluxhub.chat.ChatViewModel
import com.liquidglass.fluxhub.chat.SettingsScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val navController = rememberNavController()
            val chatViewModel: ChatViewModel = viewModel()
            
            NavHost(
                navController = navController,
                startDestination = "chat"
            ) {
                composable("chat") {
                    ChatScreen(
                        onNavigateToSettings = {
                            navController.navigate("settings")
                        },
                        viewModel = chatViewModel
                    )
                }
                
                composable("settings") {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        viewModel = chatViewModel
                    )
                }
            }
        }
    }
}
