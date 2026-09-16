package com.farfresh.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.farfresh.app.ui.FarFreshApp
import com.farfresh.app.ui.theme.FarFreshTheme

import androidx.lifecycle.lifecycleScope
import com.farfresh.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Autenticación anónima automática
        lifecycleScope.launch {
            authRepository.signInAnonymously()
        }

        enableEdgeToEdge()
        setContent {
            FarFreshTheme {
                FarFreshApp()
            }
        }
    }
}
