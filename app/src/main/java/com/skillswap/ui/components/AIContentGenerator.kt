package com.skillswap.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.skillswap.ai.CloudflareAIService
import com.skillswap.ui.theme.OrangePrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIContentGeneratorBottomSheet(
    title: String,
    description: String,
    onDescriptionGenerated: (String) -> Unit,
    onImageGenerated: (ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberStandardBottomSheetState()
    var generatedDescription by remember { mutableStateOf("") }
    var isGeneratingText by remember { mutableStateOf(false) }
    var isGeneratingImage by remember { mutableStateOf(false) }
    var textError by remember { mutableStateOf<String?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var generatedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    
    // Image generation options
    var useTitle by remember { mutableStateOf(true) }
    var useDescription by remember { mutableStateOf(true) }
    var useCustomPrompt by remember { mutableStateOf(false) }
    var customPrompt by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    
    BottomSheetContainer(
        sheetState = sheetState,
        onDismiss = onDismiss,
        title = "Générateur de contenu IA",
        subtitle = "Utilisez l'IA pour créer du contenu"
    ) {
        // Text Generation Section
        BottomSheetSection(title = "📝 Générer une description") {
            Text(
                text = "Basé sur le titre: \"$title\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (generatedDescription.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Description générée:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = generatedDescription,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isGeneratingText = true
                                textError = null
                                try {
                                    val prompt = "Écris une description engageante et professionnelle pour: $title"
                                    generatedDescription = CloudflareAIService.generateText(
                                        prompt = prompt,
                                        systemPrompt = "Tu es un expert en marketing et rédaction. Crée des descriptions concises, attrayantes et informatives en français.",
                                        maxTokens = 512,
                                        temperature = 0.8
                                    )
                                } catch (e: Exception) {
                                    textError = e.message ?: "Erreur de génération"
                                } finally {
                                    isGeneratingText = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isGeneratingText
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Régénérer")
                    }
                    
                    Button(
                        onClick = {
                            onDescriptionGenerated(generatedDescription)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Utiliser")
                    }
                }
            } else {
                BottomSheetButton(
                    text = "Générer la description",
                    onClick = {
                        scope.launch {
                            isGeneratingText = true
                            textError = null
                            try {
                                val prompt = "Écris une description engageante et professionnelle pour: $title"
                                generatedDescription = CloudflareAIService.generateText(
                                    prompt = prompt,
                                    systemPrompt = "Tu es un expert en marketing et rédaction. Crée des descriptions concises, attrayantes et informatives en français.",
                                    maxTokens = 512,
                                    temperature = 0.8
                                )
                            } catch (e: Exception) {
                                textError = e.message ?: "Erreur de génération"
                            } finally {
                                isGeneratingText = false
                            }
                        }
                    },
                    isLoading = isGeneratingText
                )
            }
            
            textError?.let {
                Text(
                    text = "⚠️ $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        BottomSheetDivider()
        
        // Image Generation Section
        BottomSheetSection(title = "🎨 Générer une image") {
            Text(
                text = "Sélectionnez les éléments pour le prompt:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Checkboxes for prompt components
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = useTitle,
                    onCheckedChange = { useTitle = it },
                    colors = CheckboxDefaults.colors(checkedColor = OrangePrimary)
                )
                Text("Utiliser le titre", style = MaterialTheme.typography.bodyMedium)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = useDescription,
                    onCheckedChange = { useDescription = it },
                    colors = CheckboxDefaults.colors(checkedColor = OrangePrimary)
                )
                Text("Utiliser la description", style = MaterialTheme.typography.bodyMedium)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = useCustomPrompt,
                    onCheckedChange = { useCustomPrompt = it },
                    colors = CheckboxDefaults.colors(checkedColor = OrangePrimary)
                )
                Text("Prompt personnalisé", style = MaterialTheme.typography.bodyMedium)
            }
            
            if (useCustomPrompt) {
                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = { customPrompt = it },
                    label = { Text("Prompt personnalisé") },
                    placeholder = { Text("Ex: modern, colorful, professional...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (generatedImageBytes != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.LightGray)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Display generated image (would need proper implementation)
                        Text(
                            text = "✓ Image générée",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isGeneratingImage = true
                                imageError = null
                                try {
                                    val promptParts = mutableListOf<String>()
                                    if (useTitle) promptParts.add(title)
                                    if (useDescription && description.isNotEmpty()) promptParts.add(description)
                                    if (useCustomPrompt && customPrompt.isNotEmpty()) promptParts.add(customPrompt)
                                    
                                    val finalPrompt = promptParts.joinToString(", ")
                                    generatedImageBytes = CloudflareAIService.generateImage(
                                        prompt = finalPrompt,
                                        numSteps = 4,
                                        guidance = 3.5
                                    )
                                } catch (e: Exception) {
                                    imageError = e.message ?: "Erreur de génération d'image"
                                } finally {
                                    isGeneratingImage = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isGeneratingImage
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Régénérer")
                    }
                    
                    Button(
                        onClick = {
                            generatedImageBytes?.let { onImageGenerated(it) }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Utiliser")
                    }
                }
            } else {
                BottomSheetButton(
                    text = "Générer l'image",
                    onClick = {
                        scope.launch {
                            isGeneratingImage = true
                            imageError = null
                            try {
                                val promptParts = mutableListOf<String>()
                                if (useTitle) promptParts.add(title)
                                if (useDescription && description.isNotEmpty()) promptParts.add(description)
                                if (useCustomPrompt && customPrompt.isNotEmpty()) promptParts.add(customPrompt)
                                
                                val finalPrompt = promptParts.joinToString(", ")
                                generatedImageBytes = CloudflareAIService.generateImage(
                                    prompt = finalPrompt,
                                    numSteps = 4,
                                    guidance = 3.5
                                )
                            } catch (e: Exception) {
                                imageError = e.message ?: "Erreur de génération d'image"
                            } finally {
                                isGeneratingImage = false
                            }
                        }
                    },
                    isLoading = isGeneratingImage,
                    enabled = useTitle || (useDescription && description.isNotEmpty()) || (useCustomPrompt && customPrompt.isNotEmpty())
                )
            }
            
            imageError?.let {
                Text(
                    text = "⚠️ $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ImagePickerField(
    selectedImageUri: Uri?,
    onImageSelected: (Uri) -> Unit,
    label: String = "Sélectionner une image",
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }
    
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable { launcher.launch("image/*") },
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Image sélectionnée",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Change image button
                    IconButton(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.White, shape = RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Changer l'image",
                            tint = OrangePrimary
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Toucher pour ajouter une image",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
