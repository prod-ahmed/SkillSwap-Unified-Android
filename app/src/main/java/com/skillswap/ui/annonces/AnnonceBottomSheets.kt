package com.skillswap.ui.annonces

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.skillswap.model.Annonce
import com.skillswap.ui.components.*
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.AnnoncesViewModel

import com.skillswap.ai.CloudflareAIService
import kotlinx.coroutines.launch

import com.skillswap.model.MediaPayload

private val TealPrimary = Color(0xFF12947D)
private val AIButtonColor = Color(0xFF5856D6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAnnonceBottomSheet(
    onDismiss: () -> Unit,
    onAnnonceCreated: () -> Unit = {},
    viewModel: AnnoncesViewModel = viewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var generatedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var generatedImageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isGeneratingDescription by remember { mutableStateOf(false) }
    var isGeneratingImage by remember { mutableStateOf(false) }
    
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val categories = listOf("Cours", "Formation", "Workshop", "Mentorat", "Autre")
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> 
        uri?.let {
            selectedImageUri = it
            generatedImageBytes = null
            generatedImageBitmap = null
            context.contentResolver.openInputStream(it)?.use { stream ->
                selectedImageBytes = stream.readBytes()
            }
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFE8F5E9), Color.White)
                    ),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(top = 16.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            )
            
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Nouvelle Annonce",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                    Text(
                        "Partagez votre savoir-faire",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.Gray)
                }
            }
            
            // Error message
            error?.let { errorMessage ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFEBEE)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFC62828))
                        Spacer(Modifier.width(8.dp))
                        Text(errorMessage, color = Color(0xFFC62828), fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                GlassInputCard {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Titre de l'annonce") },
                        placeholder = { Text("Ex: Cours de guitare débutant") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        leadingIcon = { Icon(Icons.Default.Title, null, tint = TealPrimary) }
                    )
                }
                
                // Description with AI Button
                GlassInputCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Description",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            
                            Button(
                                onClick = {
                                    if (title.isNotBlank()) {
                                        scope.launch {
                                            isGeneratingDescription = true
                                            try {
                                                description = generateAnnonceText(title, category)
                                            } finally {
                                                isGeneratingDescription = false
                                            }
                                        }
                                    }
                                },
                                enabled = title.isNotBlank() && !isGeneratingDescription,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AIButtonColor,
                                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isGeneratingDescription) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isGeneratingDescription) "..." else "IA", fontSize = 12.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Décrivez votre annonce...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
                
                // Category
                GlassInputCard {
                    Column {
                        Text(
                            "Catégorie",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.take(3).forEach { cat ->
                                CategoryChip(
                                    text = cat,
                                    isSelected = category == cat,
                                    onClick = { category = cat },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.drop(3).forEach { cat ->
                                CategoryChip(
                                    text = cat,
                                    isSelected = category == cat,
                                    onClick = { category = cat },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (categories.drop(3).size < 3) {
                                repeat(3 - categories.drop(3).size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                
                // City
                GlassInputCard {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Ville") },
                        placeholder = { Text("Ex: Tunis, Ariana") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = TealPrimary) }
                    )
                }
                
                // Image Section with AI Generation
                GlassInputCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Image",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            
                            // AI Generate Image Button
                            Button(
                                onClick = {
                                    if (title.isNotBlank()) {
                                        scope.launch {
                                            isGeneratingImage = true
                                            try {
                                                val categoryText = if (category.isNotBlank()) category else "formation"
                                                val prompt = "Professional image for: $title, $categoryText, education, learning, modern clean design"
                                                val imageBytes = CloudflareAIService.generateImage(prompt)
                                                generatedImageBytes = imageBytes
                                                generatedImageBitmap = android.graphics.BitmapFactory.decodeByteArray(
                                                    imageBytes, 0, imageBytes.size
                                                )
                                                selectedImageUri = null
                                                selectedImageBytes = null
                                            } catch (e: Exception) {
                                                android.util.Log.e("AnnonceBottomSheet", "Failed to generate image: ${e.message}")
                                            } finally {
                                                isGeneratingImage = false
                                            }
                                        }
                                    }
                                },
                                enabled = title.isNotBlank() && !isGeneratingImage,
                                colors = ButtonDefaults.buttonColors(containerColor = AIButtonColor),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isGeneratingImage) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Générer", fontSize = 12.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        when {
                            generatedImageBitmap != null -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    Image(
                                        bitmap = generatedImageBitmap!!.asImageBitmap(),
                                        contentDescription = "Image IA générée",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // AI Badge
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = AIButtonColor
                                    ) {
                                        Text(
                                            "✨ IA",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(
                                        onClick = { 
                                            generatedImageBytes = null
                                            generatedImageBitmap = null
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.White)
                                    }
                                }
                            }
                            generatedImageBytes != null -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Gray.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.CheckCircle, null, tint = Color.Green, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("✨ Image IA générée", color = AIButtonColor, fontWeight = FontWeight.Medium)
                                    }
                                    IconButton(
                                        onClick = { generatedImageBytes = null },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                                    }
                                }
                            }
                            selectedImageUri != null -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { 
                                            selectedImageUri = null
                                            selectedImageBytes = null
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.White)
                                    }
                                }
                            }
                            else -> {
                                OutlinedButton(
                                    onClick = { imagePicker.launch("image/*") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AddPhotoAlternate, null, tint = TealPrimary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Choisir une image", color = Color.Gray, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Submit Button
            Button(
                onClick = {
                    isSubmitting = true
                    val media = when {
                        generatedImageBytes != null -> MediaPayload(
                            bytes = generatedImageBytes!!,
                            filename = "ai_annonce_${System.currentTimeMillis()}.jpg",
                            mimeType = "image/jpeg"
                        )
                        selectedImageBytes != null -> MediaPayload(
                            bytes = selectedImageBytes!!,
                            filename = "annonce_${System.currentTimeMillis()}.jpg",
                            mimeType = "image/jpeg"
                        )
                        else -> null
                    }
                    
                    viewModel.createAnnonce(
                        title = title,
                        description = description,
                        city = city.ifBlank { null },
                        category = category.ifEmpty { null },
                        media = media
                    )
                    // Note: Don't dismiss here - wait for success state
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .height(56.dp),
                enabled = !isSubmitting && !isLoading && title.isNotBlank() && description.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                if (isSubmitting || isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Publish, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publier l'annonce", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
    
    // Observe success to dismiss
    val success by viewModel.success.collectAsState()
    LaunchedEffect(success) {
        if (success != null) {
            onAnnonceCreated()
            onDismiss()
            viewModel.clearMessages()
        }
    }
}

@Composable
private fun GlassInputCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}

@Composable
private fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TealPrimary else Color.White.copy(alpha = 0.5f),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)) else null,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isSelected) Color.White else Color.Gray,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

private suspend fun generateAnnonceText(title: String, category: String): String {
    kotlinx.coroutines.delay(800)
    val categoryText = if (category.isNotBlank()) category.lowercase() else "formation"
    return """📚 $title

🎯 Ce $categoryText est conçu pour vous aider à développer vos compétences.

✅ Ce que vous apprendrez:
• Les fondamentaux et concepts clés
• Techniques pratiques et exercices
• Conseils personnalisés

📍 Contactez-moi pour plus d'informations!"""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAnnonceBottomSheet(
    annonce: Annonce,
    onDismiss: () -> Unit,
    onAnnonceUpdated: () -> Unit = {},
    viewModel: AnnoncesViewModel = viewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var title by remember { mutableStateOf(annonce.title) }
    var description by remember { mutableStateOf(annonce.description) }
    var category by remember { mutableStateOf(annonce.category ?: "") }
    var city by remember { mutableStateOf(annonce.city ?: "") }
    var isSubmitting by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Modifier l'annonce",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TealPrimary
            )
            
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )
            
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("Ville") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            Button(
                onClick = {
                    isSubmitting = true
                    viewModel.updateAnnonce(
                        id = annonce.id,
                        title = title,
                        description = description,
                        city = city.ifBlank { null },
                        category = category.ifEmpty { null },
                        media = null
                    )
                    onAnnonceUpdated()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSubmitting && title.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Sauvegarder", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
