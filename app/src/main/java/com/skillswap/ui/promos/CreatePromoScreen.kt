package com.skillswap.ui.promos

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
import androidx.compose.ui.draw.blur
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
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.PromosViewModel
import com.skillswap.ui.components.*
import com.skillswap.model.MediaPayload
import kotlinx.coroutines.launch
import java.time.LocalDate

private val GlassWhite = Color.White.copy(alpha = 0.85f)
private val GlassBorder = Color.White.copy(alpha = 0.3f)
private val OrangeGradient = Brush.linearGradient(listOf(Color(0xFFFF9500), Color(0xFFFF6B00)))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePromoScreen(
    onBack: () -> Unit,
    onPromoCreated: () -> Unit,
    viewModel: PromosViewModel = viewModel()
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf<List<String>>(emptyList()) }
    var discountPercent by remember { mutableStateOf("") }
    var validUntil by remember { mutableStateOf<LocalDate?>(null) }
    var code by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showSkillsPicker by remember { mutableStateOf(false) }
    var isGeneratingContent by remember { mutableStateOf(false) }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val generatingImage by viewModel.generatingImage.collectAsState()
    val generatedImageUrl by viewModel.generatedImageUrl.collectAsState()
    val success by viewModel.success.collectAsState()
    val error by viewModel.error.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(success) {
        if (success != null) {
            onPromoCreated()
            viewModel.clearMessages()
        }
    }
    
    // Image picker launcher
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            // Read bytes for moderation
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
                    listOf(Color(0xFFFFF5EB), Color(0xFFF2F2F7))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Custom Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFFFF9500), Color(0xFFFF6B00)))
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
                        "Nouvelle Promotion",
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
                            "Titre de la promotion",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Ex: Promo rentrée -20%", color = Color.Gray.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
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
                                                val generatedDesc = generatePromoDescription(title, discountPercent)
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
                            placeholder = { Text("Décrivez votre promotion...", color = Color.Gray.copy(alpha = 0.6f)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                focusedContainerColor = Color.White.copy(alpha = 0.5f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
                
                // Discount & Code Row - Glassy
                GlassCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Réduction (%)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = discountPercent,
                                onValueChange = { discountPercent = it.filter { c -> c.isDigit() } },
                                placeholder = { Text("20") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Text("🏷️", fontSize = 18.sp)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                                )
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Code promo",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it.uppercase() },
                                placeholder = { Text("PROMO20") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
                
                // Valid Until - Glassy
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Valable jusqu'au",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DatePickerField(
                            selectedDate = validUntil,
                            onDateSelected = { validUntil = it },
                            label = "Date d'expiration",
                            minDate = LocalDate.now()
                        )
                    }
                }
                
                // Image Picker - Glassy
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Image de la promotion",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            
                            // AI Generate Image Button
                            Button(
                                onClick = {
                                    if (title.isNotBlank()) {
                                        scope.launch {
                                            viewModel.generatePromoImage("Promotional banner for: $title, discount offer, modern design")
                                        }
                                    }
                                },
                                enabled = title.isNotBlank() && !generatingImage,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF5856D6)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (generatingImage) {
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
                                Text("Générer image", fontSize = 12.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Image preview or picker
                        if (generatedImageUrl != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = generatedImageUrl,
                                    contentDescription = "Image générée",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Badge
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF5856D6)
                                ) {
                                    Text(
                                        "✨ IA",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                                // Clear button
                                IconButton(
                                    onClick = { viewModel.clearGeneratedImage() },
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White)
                                }
                            }
                        } else if (selectedImageUri != null) {
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
                                    Icon(Icons.Default.Close, null, tint = Color.White)
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
                                        tint = OrangePrimary,
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
                                filename = "promo_image.jpg"
                            )
                        }
                        viewModel.createPromo(
                            title = title,
                            description = description,
                            discount = discountPercent.toIntOrNull() ?: 0,
                            validTo = validUntil?.toString() ?: "",
                            promoCode = code.ifBlank { null },
                            imageUrl = generatedImageUrl,
                            media = media
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangePrimary
                    ),
                    enabled = title.isNotBlank() && description.isNotBlank() && !isLoading
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
                            "Publier la promotion",
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
            maxSelections = 3,
            title = "Compétences concernées"
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

private suspend fun generatePromoDescription(title: String, discount: String): String {
    // Simple template-based generation (can be replaced with actual AI call)
    kotlinx.coroutines.delay(1000) // Simulate API call
    val discountText = if (discount.isNotBlank()) "$discount%" else "spéciale"
    return """🎉 Profitez de notre offre $discountText sur "$title"!

✨ Offre limitée - Ne manquez pas cette opportunité exceptionnelle!

📅 Réservez dès maintenant et bénéficiez de cette réduction exclusive.

💡 Améliorez vos compétences avec les meilleurs mentors de SkillSwap."""
}
