package com.example.onlinetts.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.onlinetts.ui.settings.SettingsScreen
import com.example.onlinetts.ui.voiceparams.VoiceParamsScreen
import com.example.onlinetts.ui.voiceselection.VoiceSelectionScreen

object Routes {
    const val SETTINGS = "settings"
    const val VOICE_SELECTION = "voice_selection"
    const val VOICE_PARAMS = "voice_params"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.SETTINGS) {
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateToVoiceSelection = { navController.navigate(Routes.VOICE_SELECTION) },
                onNavigateToVoiceParams = { navController.navigate(Routes.VOICE_PARAMS) },
            )
        }
        composable(Routes.VOICE_SELECTION) {
            VoiceSelectionScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Routes.VOICE_PARAMS) {
            VoiceParamsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
