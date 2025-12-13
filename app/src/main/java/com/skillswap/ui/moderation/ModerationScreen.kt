package com.skillswap.ui.moderation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.viewmodel.ModerationViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ModerationScreen(viewModel: ModerationViewModel = viewModel()) {
    var imageBase64 by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val result by viewModel.result.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Modération") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Vérifiez une image (base64) avant de la publier.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            OutlinedTextField(
                value = imageBase64,
                onValueChange = { imageBase64 = it },
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                label = { Text("Image en base64") },
                minLines = 4
            )
            Button(onClick = { viewModel.checkImage(imageBase64) }, enabled = imageBase64.isNotBlank() && !isLoading) {
                Text("Vérifier")
            }
            if (isLoading) {
                CircularProgressIndicator()
            }
            result?.let {
                Text(
                    text = if (it.safe) "Image acceptée" else "Image refusée: ${it.reasons?.joinToString() ?: ""}",
                    fontWeight = FontWeight.Bold,
                    color = if (it.safe) Color(0xFF1B5E20) else Color.Red
                )
            }
            error?.let { Text(it, color = Color.Red) }
        }
    }
}
