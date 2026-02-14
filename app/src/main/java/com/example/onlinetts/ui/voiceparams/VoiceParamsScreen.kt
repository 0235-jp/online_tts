package com.example.onlinetts.ui.voiceparams

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.onlinetts.R
import com.example.onlinetts.data.model.VoiceParams
import com.example.onlinetts.ui.components.SliderWithLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceParamsScreen(
    onNavigateBack: () -> Unit,
    viewModel: VoiceParamsViewModel = hiltViewModel(),
) {
    val voiceParams by viewModel.voiceParams.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.voice_params)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SliderWithLabel(
                label = stringResource(R.string.speed_scale),
                value = voiceParams.speedScale,
                onValueChange = { viewModel.updateSpeedScale(it) },
                valueRange = VoiceParams.SPEED_RANGE,
            )

            Spacer(modifier = Modifier.height(8.dp))

            SliderWithLabel(
                label = stringResource(R.string.pitch_scale),
                value = voiceParams.pitchScale,
                onValueChange = { viewModel.updatePitchScale(it) },
                valueRange = VoiceParams.PITCH_RANGE,
            )

            Spacer(modifier = Modifier.height(8.dp))

            SliderWithLabel(
                label = stringResource(R.string.volume_scale),
                value = voiceParams.volumeScale,
                onValueChange = { viewModel.updateVolumeScale(it) },
                valueRange = VoiceParams.VOLUME_RANGE,
            )

            Spacer(modifier = Modifier.height(8.dp))

            SliderWithLabel(
                label = stringResource(R.string.intonation_scale),
                value = voiceParams.intonationScale,
                onValueChange = { viewModel.updateIntonationScale(it) },
                valueRange = VoiceParams.INTONATION_RANGE,
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { viewModel.resetToDefaults() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("デフォルトに戻す")
            }
        }
    }
}
