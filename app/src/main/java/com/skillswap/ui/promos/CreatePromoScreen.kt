package com.skillswap.ui.promos

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.PromosViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.launch
import com.skillswap.ui.components.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePromoScreen(
    onBack: () -> Unit,
    onPromoCreated: () -> Unit,
    viewModel: PromosViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf<List<String>>(emptyList()) }
    var discountPercent by remember { mutableStateOf("") }
    var validUntil by remember { mutableStateOf<LocalDate?>(null) }
    var code by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showSkillsPicker by remember { mutableStateOf(false) }
    var showAIGenerator by remember { mutableStateOf(false) }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val generatingImage by viewModel.generatingImage.collectAsState()
    val generatedImageUrl by viewModel.generatedImageUrl.collectAsState()
    val success by viewModel.success.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(success) {
        if (success != null) {
            onPromoCreated()
            viewModel.clearMessages()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Créer une promotion") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Titre de la promotion",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Ex: Promo rentrée -20%") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Description",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            IconButton(onClick = { showAIGenerator = true }) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = "Générer avec IA",
                                    tint = OrangePrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Décrivez votre promotion...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            maxLines = 4
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Compétences concernées",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showSkillsPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (skills.isEmpty()) "Sélectionner des compétences" 
                                else "${skills.size} compétence(s) sélectionnée(s)"
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Réduction (%)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = discountPercent,
                                    onValueChange = { discountPercent = it },
                                    placeholder = { Text("20") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Code promo",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = code,
                                    onValueChange = { code = it.uppercase() },
                                    placeholder = { Text("PROMO20") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Valable jusqu'au",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DatePickerField(
                            selectedDate = validUntil,
                            onDateSelected = { validUntil = it },
                            label = "Date d'expiration",
                            minDate = LocalDate.now()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ImagePickerField(
                            selectedImageUri = selectedImageUri,
                            onImageSelected = { selectedImageUri = it },
                            label = "Image de la promotion"
                        )
                        
                        if (generatedImageUrl != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Image générée par IA",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    AsyncImage(
                                        model = generatedImageUrl,
                                        contentDescription = "Image générée",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Bottom button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        viewModel.createPromo(
                            title = title,
                            description = description,
                            discount = discountPercent.toIntOrNull() ?: 0,
                            validTo = validUntil?.toString() ?: "",
                            promoCode = code.ifBlank { null },
                            imageUrl = generatedImageUrl
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    enabled = title.isNotBlank() && description.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Créer la promotion",
                            modifier = Modifier.padding(vertical = 8.dp)
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
    
    // AI Content Generator Bottom Sheet
    if (showAIGenerator) {
        AIContentGeneratorBottomSheet(
            title = title,
            description = description,
            onDescriptionGenerated = { generatedDesc ->
                description = generatedDesc
            },
            onImageGenerated = { imageBytes ->
                // Handle image generation
                scope.launch {
                    // Convert bytes to URL or save locally
                }
            },
            onDismiss = { showAIGenerator = false }
        )
    }
}
