package com.valoes.valesaquagestor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.valoes.valesaquagestor.presentation.navigation.AquaGestorApp
import com.valoes.valesaquagestor.presentation.theme.AquaGestorTheme
import dagger.hilt.android.AndroidEntryPoint

/** Única Activity do app: hospeda o tema e a navegação (Compose). */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AquaGestorTheme {
                AquaGestorApp()
            }
        }
    }
}
