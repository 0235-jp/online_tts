package com.example.onlinetts.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.onlinetts.ui.settings.SettingsScreen
import com.example.onlinetts.ui.speaker.SpeakerSelectionScreen
import com.example.onlinetts.ui.voiceparams.VoiceParamsScreen

object Routes {
    const val SETTINGS = "settings"
    const val SPEAKER_SELECTION = "speaker_selection"
    const val VOICE_PARAMS = "voice_params"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.SETTINGS) {
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateToSpeakerSelection = { navController.navigate(Routes.SPEAKER_SELECTION) },
                onNavigateToVoiceParams = { navController.navigate(Routes.VOICE_PARAMS) },
            )
        }
        composable(Routes.SPEAKER_SELECTION) {
            SpeakerSelectionScreen(
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
