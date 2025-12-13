package com.skillswap.ui.annonces

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.skillswap.model.Annonce
import com.skillswap.model.MediaPayload
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.AnnoncesViewModel
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAnnonceScreen(
    annonce: Annonce,
    onBack: () -> Unit,
    onSave: () -> Unit,
    viewModel: AnnoncesViewModel = viewModel()
) {
    val context = LocalContext.current
    
    var title by remember { mutableStateOf(annonce.title) }
    var description by remember { mutableStateOf(annonce.description ?: "") }
    var city by remember { mutableStateOf(annonce.city ?: "") }
    var category by remember { mutableStateOf(annonce.category ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var mediaPayload by remember { mutableStateOf<MediaPayload?>(null) }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.success.collectAsState()
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            imageUri = it
            // Convert URI to MediaPayload
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                
                if (bytes != null) {
                    val fileName = "annonce_${System.currentTimeMillis()}.jpg"
                    val mimeType = context.contentResolver.getType(it) ?: "image/jpeg"
                    mediaPayload = MediaPayload(
                        fileName = fileName,
                        mimeType = mimeType,
                        bytes = bytes
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Handle success
    LaunchedEffect(success) {
        if (success != null) {
            onSave()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifier l'annonce") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 16.dp)
                        )
                    } else {
                        TextButton(
                            onClick = {
                                viewModel.updateAnnonce(
                                    id = annonce.id,
                                    title = title,
                                    description = description,
                                    city = city.ifEmpty { null },
                                    media = mediaPayload
                                )
                            },
                            enabled = title.isNotBlank() && description.isNotBlank()
                        ) {
                            Text("Enregistrer", color = OrangePrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )
            
            // City
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("Ville (optionnel)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Category
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Catégorie (optionnel)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Image Section
            Text("Image", style = MaterialTheme.typography.titleMedium)
            
            Button(
                onClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Changer l'image")
            }
            
            // Display selected or current image
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            } else if (annonce.imageUrl != null) {
                AsyncImage(
                    model = annonce.imageUrl,
                    contentDescription = "Current image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
            
            // Error message
            error?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
