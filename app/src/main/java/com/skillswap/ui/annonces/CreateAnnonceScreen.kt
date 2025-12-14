package com.skillswap.ui.annonces

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
import com.skillswap.viewmodel.AnnoncesViewModel
import com.skillswap.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAnnonceScreen(
    onBack: () -> Unit,
    onAnnonceCreated: () -> Unit,
    viewModel: AnnoncesViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf<List<String>>(emptyList()) }
    var category by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showSkillsPicker by remember { mutableStateOf(false) }
    var showAIGenerator by remember { mutableStateOf(false) }
    
    val categories = listOf("Cours", "Formation", "Workshop", "Mentorat", "Autre")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Créer une annonce") },
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
                            "Titre de l'annonce",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Ex: Cours de guitare débutant") },
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
                            placeholder = { Text("Décrivez votre annonce...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5,
                            trailingIcon = {
                                if (description.isEmpty() && title.isNotEmpty()) {
                                    IconButton(onClick = { showAIGenerator = true }) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = "Générer avec IA",
                                            tint = OrangePrimary
                                        )
                                    }
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Compétences associées",
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
                        
                        if (skills.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                skills.take(3).forEach { skill ->
                                    AssistChip(
                                        onClick = { },
                                        label = { Text(skill) }
                                    )
                                }
                                if (skills.size > 3) {
                                    AssistChip(
                                        onClick = { showSkillsPicker = true },
                                        label = { Text("+${skills.size - 3}") }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Catégorie",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        categories.chunked(2).forEach { rowCategories ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowCategories.forEach { cat ->
                                    val isSelected = category == cat
                                    Button(
                                        onClick = { category = cat },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) OrangePrimary
                                            else Color(0xFFF5F5F5)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            cat,
                                            color = if (isSelected) Color.White else Color.Black,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                if (rowCategories.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "Prix (optionnel)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            placeholder = { Text("Ex: 50 DT/heure") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Localisation",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            placeholder = { Text("Ex: Tunis, Ariana") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, "Location")
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ImagePickerField(
                            selectedImageUri = selectedImageUri,
                            onImageSelected = { selectedImageUri = it },
                            label = "Image de l'annonce"
                        )
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
                        // Create annonce logic
                        isLoading = true
                        onAnnonceCreated()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    enabled = title.isNotBlank() && description.isNotBlank() && category.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Publier l'annonce",
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
            maxSelections = 5,
            title = "Sélectionner des compétences"
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
                // Handle image bytes (convert to Uri if needed)
            },
            onDismiss = { showAIGenerator = false }
        )
    }
}
