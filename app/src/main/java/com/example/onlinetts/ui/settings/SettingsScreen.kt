package com.example.onlinetts.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.onlinetts.R
import com.example.onlinetts.tts.api.AudioQueryRequest
import com.example.onlinetts.tts.provider.TtsProviderType
import com.example.onlinetts.ui.components.TestSpeechButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToSpeakerSelection: () -> Unit,
    onNavigateToVoiceParams: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings_title)) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // API Provider Selection
            Text(
                text = stringResource(R.string.api_provider),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            var providerExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = providerExpanded,
                onExpandedChange = { providerExpanded = it },
            ) {
                OutlinedTextField(
                    value = uiState.settings.providerType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = providerExpanded,
                    onDismissRequest = { providerExpanded = false },
                ) {
                    TtsProviderType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                viewModel.updateProviderType(type)
                                providerExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // API Key
            Text(
                text = stringResource(R.string.api_key),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = uiState.apiKey,
                    onValueChange = { viewModel.updateApiKey(it) },
                    placeholder = { Text(stringResource(R.string.api_key_hint)) },
                    visualTransformation = if (uiState.isApiKeyVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.saveApiKey() }) {
                    Text(stringResource(R.string.save))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Speaker Selection
            ListItem(
                headlineContent = { Text(stringResource(R.string.speaker_selection)) },
                supportingContent = {
                    val name = uiState.settings.selectedSpeakerName
                    val uuid = uiState.settings.speakerModelUuid
                    if (name.isNotBlank()) {
                        Text("$name\nUUID: $uuid")
                    } else if (uuid.isNotBlank()) {
                        Text("UUID: $uuid")
                    }
                },
                modifier = Modifier.clickable { onNavigateToSpeakerSelection() },
            )

            HorizontalDivider()

            // Voice Params
            ListItem(
                headlineContent = { Text(stringResource(R.string.voice_params)) },
                modifier = Modifier.clickable { onNavigateToVoiceParams() },
            )

            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // Test Speech
            TestSpeechButton(
                provider = remember(uiState.settings.providerType) { viewModel.getProvider() },
                request = AudioQueryRequest(
                    text = stringResource(R.string.test_text),
                    speakerId = uiState.settings.selectedSpeakerId,
                    voiceParams = uiState.settings.voiceParams,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
