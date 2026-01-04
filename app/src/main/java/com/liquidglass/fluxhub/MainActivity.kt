package com.liquidglass.fluxhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liquidglass.fluxhub.chat.ChatViewModel
import com.liquidglass.fluxhub.chat.MainScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val chatViewModel: ChatViewModel = viewModel()
            MainScreen(viewModel = chatViewModel)
        }
    }
}
