package com.example.onlinetts.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.onlinetts.ui.navigation.NavGraph
import com.example.onlinetts.ui.theme.OnlineTtsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnlineTtsTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
