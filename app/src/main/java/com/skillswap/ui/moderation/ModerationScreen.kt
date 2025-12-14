package com.skillswap.ui.moderation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.viewmodel.ModerationViewModel
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ModerationScreen(viewModel: ModerationViewModel = viewModel()) {
    val context = LocalContext.current
    var selectedImageBase64 by remember { mutableStateOf<String?>(null) }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val isLoading by viewModel.isLoading.collectAsState()
    val result by viewModel.result.collectAsState()
    val error by viewModel.error.collectAsState()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                previewBitmap = bitmap
                
                // Convert to base64
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
                val bytes = outputStream.toByteArray()
                selectedImageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            } catch (e: Exception) {
                selectedImageBase64 = null
                previewBitmap = null
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Modération") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Vérifiez une image avant de la publier.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            // Image preview
            if (previewBitmap != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Image sélectionnée",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // Pick Image Button
            OutlinedButton(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (previewBitmap == null) "Choisir une image" else "Changer l'image")
            }
            
            // Verify Button
            Button(
                onClick = { selectedImageBase64?.let { viewModel.checkImage(it) } },
                enabled = selectedImageBase64 != null && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Vérifier")
            }
            
            if (isLoading) {
                CircularProgressIndicator()
            }
            
            result?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (it.safe) "✅ Image acceptée" else "❌ Image refusée",
                            fontWeight = FontWeight.Bold,
                            color = if (it.safe) Color(0xFF1B5E20) else Color.Red,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (!it.safe && it.reasons != null) {
                            Text(
                                "Raisons: ${it.reasons.joinToString(", ")}",
                                color = Color.Red,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            error?.let {
                Text(it, color = Color.Red)
            }
        }
    }
}
