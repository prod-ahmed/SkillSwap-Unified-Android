package com.skillswap.ui.promos

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillswap.model.Promo
import com.skillswap.ui.components.*
import com.skillswap.ui.theme.OrangePrimary
import com.skillswap.viewmodel.PromosViewModel

import com.skillswap.ai.CloudflareAIService
import com.skillswap.model.MediaPayload
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePromoBottomSheet(
    onDismiss: () -> Unit,
    onPromoCreated: () -> Unit = {},
    viewModel: PromosViewModel = viewModel()
) {
    val sheetState = rememberStandardBottomSheetState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("") }
    var promoCode by remember { mutableStateOf("") }
    var validUntil by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var generatedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isGeneratingAI by remember { mutableStateOf(false) }
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }
    
    BottomSheetContainer(
        sheetState = sheetState,
        onDismiss = onDismiss,
        title = "Créer une promotion",
        subtitle = "Offrez une réduction à vos clients"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BottomSheetTextField(
                value = title,
                onValueChange = { title = it },
                label = "Titre",
                placeholder = "Ex: Promo Black Friday",
                leadingIcon = { Icon(Icons.Default.LocalOffer, null) }
            )
            
            // AI Description
            if (title.isNotBlank() && discount.isNotBlank()) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isGeneratingAI = true
                            try {
                                val prompt = "Write a short, persuasive description for a promotion titled '$title' with $discount% discount."
                                val generated = CloudflareAIService.generateText(prompt, maxTokens = 200)
                                description = generated
                            } catch (e: Exception) {
                                // Handle error
                            } finally {
                                isGeneratingAI = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGeneratingAI
                ) {
                    if (isGeneratingAI) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Générer la description avec IA")
                }
            }
            
            BottomSheetTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                placeholder = "Décrivez votre promotion...",
                singleLine = false,
                maxLines = 3,
                leadingIcon = { Icon(Icons.Default.Description, null) }
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BottomSheetTextField(
                    value = discount,
                    onValueChange = { if (it.all { c -> c.isDigit() }) discount = it },
                    label = "Réduction (%)",
                    placeholder = "20",
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Percent, null) }
                )
                
                BottomSheetTextField(
                    value = promoCode,
                    onValueChange = { promoCode = it.uppercase() },
                    label = "Code promo",
                    placeholder = "PROMO20",
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Code, null) }
                )
            }
            
            BottomSheetTextField(
                value = validUntil,
                onValueChange = { validUntil = it },
                label = "Valable jusqu'au (YYYY-MM-DD)",
                placeholder = "2025-12-31",
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) }
            )
            
            // Image picker & AI Image
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Icon(if (selectedImageUri != null) Icons.Default.CheckCircle else Icons.Default.Image, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedImageUri != null) "Image sélectionnée" else "Ajouter une image")
                }
                
                if (title.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isGeneratingAI = true
                                try {
                                    val prompt = "A promotional image for '$title' with $discount% off. $description"
                                    val imageBytes = CloudflareAIService.generateImage(prompt)
                                    generatedImageBytes = imageBytes
                                    selectedImageUri = null
                                } catch (e: Exception) {
                                    // Handle error
                                } finally {
                                    isGeneratingAI = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isGeneratingAI
                    ) {
                        if (isGeneratingAI) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (generatedImageBytes != null) "Image générée (cliquer pour régénérer)" else "Générer une image avec IA")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    isSubmitting = true
                    val media = when {
                        generatedImageBytes != null -> MediaPayload(
                            bytes = generatedImageBytes!!,
                            filename = "ai_promo_${System.currentTimeMillis()}.jpg",
                            mimeType = "image/jpeg"
                        )
                        else -> null
                    }
                    
                    viewModel.createPromo(
                        title = title,
                        description = description,
                        discount = discount.toIntOrNull() ?: 0,
                        validTo = validUntil,
                        promoCode = promoCode.ifEmpty { null },
                        media = media
                    )
                    onPromoCreated()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && title.isNotBlank() && discount.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Créer la promotion")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPromoBottomSheet(
    promo: Promo,
    onDismiss: () -> Unit,
    onPromoUpdated: () -> Unit = {},
    viewModel: PromosViewModel = viewModel()
) {
    val sheetState = rememberStandardBottomSheetState()
    val context = LocalContext.current
    
    var title by remember { mutableStateOf(promo.title) }
    var description by remember { mutableStateOf(promo.description) }
    var discount by remember { mutableStateOf(promo.discount.toString()) }
    var promoCode by remember { mutableStateOf(promo.promoCode ?: "") }
    var validUntil by remember { mutableStateOf(promo.validUntil) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }
    
    BottomSheetContainer(
        sheetState = sheetState,
        onDismiss = onDismiss,
        title = "Modifier la promotion",
        subtitle = "Mettez à jour votre promotion"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BottomSheetTextField(
                value = title,
                onValueChange = { title = it },
                label = "Titre",
                leadingIcon = { Icon(Icons.Default.LocalOffer, null) }
            )
            
            BottomSheetTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                singleLine = false,
                maxLines = 3,
                leadingIcon = { Icon(Icons.Default.Description, null) }
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BottomSheetTextField(
                    value = discount,
                    onValueChange = { if (it.all { c -> c.isDigit() }) discount = it },
                    label = "Réduction (%)",
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Percent, null) }
                )
                
                BottomSheetTextField(
                    value = promoCode,
                    onValueChange = { promoCode = it.uppercase() },
                    label = "Code promo",
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Code, null) }
                )
            }
            
            BottomSheetTextField(
                value = validUntil,
                onValueChange = { validUntil = it },
                label = "Valable jusqu'au (YYYY-MM-DD)",
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) }
            )
            
            Button(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Icon(if (selectedImageUri != null) Icons.Default.CheckCircle else Icons.Default.Image, null)
                Spacer(Modifier.width(8.dp))
                Text(if (selectedImageUri != null) "Image sélectionnée" else "Changer l'image")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    isSubmitting = true
                    viewModel.updatePromo(
                        id = promo.id,
                        title = title,
                        description = description,
                        discount = discount.toIntOrNull() ?: 0,
                        validTo = validUntil,
                        promoCode = promoCode.ifEmpty { null },
                        media = null
                    )
                    onPromoUpdated()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Sauvegarder")
                }
            }
        }
    }
}
