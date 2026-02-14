package com.example.onlinetts.ui.voiceselection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.onlinetts.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSelectionScreen(
    onNavigateBack: () -> Unit,
    viewModel: VoiceSelectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.voice_selection)) },
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
                .padding(16.dp),
        ) {
            Text(
                text = uiState.voiceSearchLabel,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.updateQuery(it) },
                    placeholder = { Text("検索クエリを入力") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.search() },
                    enabled = !uiState.isLoading,
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("取得")
                    }
                }
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (uiState.voices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.voices) { voice ->
                        ListItem(
                            headlineContent = { Text(voice.name) },
                            supportingContent = {
                                if (voice.description.isNotBlank()) {
                                    Text(voice.description)
                                }
                            },
                            trailingContent = {
                                if (voice.id == uiState.selectedVoiceId) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectVoice(voice.id) },
                        )
                        HorizontalDivider()
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (uiState.saved) "保存しました" else stringResource(R.string.save))
                }
            }
        }
    }
}
