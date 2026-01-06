package com.skillswap.ui.annonces

import android.net.Uri
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.skillswap.model.MediaPayload
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.AnnoncesViewModel
import com.skillswap.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAnnonceScreen(
    onBack: () -> Unit,
    onAnnonceCreated: () -> Unit,
    viewModel: AnnoncesViewModel = viewModel()
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf<List<String>>(emptyList()) }
    var category by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showSkillsPicker by remember { mutableStateOf(false) }
    var isGeneratingContent by remember { mutableStateOf(false) }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val success by viewModel.success.collectAsState()
    val error by viewModel.error.collectAsState()
    val scope = rememberCoroutineScope()
    
    val categories = listOf("Cours", "Formation", "Workshop", "Mentorat", "Autre")
    
    LaunchedEffect(success) {
        if (success != null) {
            onAnnonceCreated()
            viewModel.clearMessages()
        }
    }
    
    // Image picker launcher
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            context.contentResolver.openInputStream(it)?.use { stream ->
                selectedImageBytes = stream.readBytes()
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFE8F5E9), Color(0xFFF2F2F7))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Custom Header - Teal gradient for annonces
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF12947D), Color(0xFF0D7A68)))
                    )
                    .padding(top = 48.dp, bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour", tint = Color.White)
                    }
                    Text(
                        "Nouvelle Annonce",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error message
                error?.let { errorMessage ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
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
                }
                
                // Title Card - Glassy
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Titre de l'annonce",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Ex: Cours de guitare débutant", color = Color.Gray.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF12947D),
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                focusedContainerColor = Color.White.copy(alpha = 0.5f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
                
                // Description Card with AI Generate Button - Glassy
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Description",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            
                            // AI Generate Button
                            Button(
                                onClick = {
                                    if (title.isNotBlank()) {
                                        isGeneratingContent = true
                                        scope.launch {
                                            try {
                                                val generatedDesc = generateAnnonceDescription(title, category)
                                                description = generatedDesc
                                            } finally {
                                                isGeneratingContent = false
                                            }
                                        }
                                    }
                                },
                                enabled = title.isNotBlank() && !isGeneratingContent,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF5856D6),
                                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isGeneratingContent) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isGeneratingContent) "Génération..." else "Générer avec IA",
                                    fontSize = 12.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Décrivez votre annonce...", color = Color.Gray.copy(alpha = 0.6f)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF12947D),
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                focusedContainerColor = Color.White.copy(alpha = 0.5f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
                
                // Category Selection - Glassy
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Catégorie",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
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
                            // Fill remaining space
                            if (categories.drop(3).size == 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                
                // Skills - Glassy
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Compétences associées",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showSkillsPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF12947D)
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (skills.isEmpty()) "Sélectionner des compétences" 
                                else "${skills.size} compétence(s) sélectionnée(s)"
                            )
                        }
                        
                        if (skills.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                skills.take(3).forEach { skill ->
                                    AssistChip(
                                        onClick = { },
                                        label = { Text(skill, fontSize = 12.sp) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = Color(0xFF12947D).copy(alpha = 0.1f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Price & Location - Glassy
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Prix (optionnel)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            placeholder = { Text("Ex: 50 DT/heure") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Text("💰", fontSize = 18.sp)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF12947D),
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Localisation",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            placeholder = { Text("Ex: Tunis, Ariana") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.LocationOn,
                                    "Location",
                                    tint = Color(0xFF12947D)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF12947D),
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
                
                // Image Picker - Glassy
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Image de l'annonce",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (selectedImageUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Image sélectionnée",
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
                                        .padding(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.Black.copy(alpha = 0.5f)
                                    ) {
                                        Icon(
                                            Icons.Default.Close, 
                                            null, 
                                            tint = Color.White,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color.Gray.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = Color(0xFF12947D),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Ajouter une image",
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(80.dp))
            }
            
            // Bottom button - Floating style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.95f), Color.White)
                        )
                    )
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        val media = selectedImageBytes?.let { bytes ->
                            MediaPayload(
                                bytes = bytes,
                                mimeType = "image/jpeg",
                                filename = "annonce_image.jpg"
                            )
                        }
                        viewModel.createAnnonce(
                            title = title,
                            description = description,
                            city = location.ifBlank { null },
                            category = category.ifBlank { null },
                            media = media
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF12947D)
                    ),
                    enabled = title.isNotBlank() && description.isNotBlank() && category.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Publish, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Publier l'annonce",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
    
    // Skills Picker Bottom Sheet
    if (showSkillsPicker) {
        SkillsPickerBottomSheet(
            selectedSkills = skills,
            onSkillsChanged = { skills = it },
            onDismiss = { showSkillsPicker = false },
            maxSelections = 5,
            title = "Sélectionner des compétences"
        )
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.75f),
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.5f)
        )
    ) {
        content()
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
        color = if (isSelected) Color(0xFF12947D) else Color.White.copy(alpha = 0.5f),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.Gray.copy(alpha = 0.3f)
        ) else null,
        onClick = onClick
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

private suspend fun generateAnnonceDescription(title: String, category: String): String {
    kotlinx.coroutines.delay(1000) // Simulate API call
    val categoryText = if (category.isNotBlank()) category.lowercase() else "formation"
    return """📚 $title

🎯 Ce $categoryText est conçu pour vous aider à développer vos compétences de manière pratique et efficace.

✅ Ce que vous apprendrez:
• Les fondamentaux et concepts clés
• Techniques pratiques et exercices
• Conseils personnalisés

👨‍🏫 Méthode d'enseignement adaptée à votre niveau et vos objectifs.

📍 Contactez-moi pour plus d'informations!"""
}
